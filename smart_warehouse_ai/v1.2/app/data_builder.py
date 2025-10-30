# app/data_builder.py
import pandas as pd
from sqlalchemy import create_engine


class DataBuilder:
    """
    Builds dataset from inventory_history + products.
    Features: quantity, expected_quantity, difference, min_stock, optimal_stock
    Targets: days_until_stockout, recommended_order
    """

    def __init__(self, db_url: str):
        self.engine = create_engine(db_url)

    def build_dataset(self):
        query = """
        SELECT
            ih.id AS inventory_id,
            ih.product_id,
            ih.warehouse_id,
            ih.quantity,
            ih.expected_quantity,
            ih.difference,
            ih.scanned_at,
            p.min_stock,
            p.optimal_stock
        FROM inventory_history ih
        JOIN products p ON p.id = ih.product_id
        WHERE ih.is_deleted = false
          AND p.is_deleted = false
        ORDER BY ih.product_id, ih.scanned_at
        """

        df = pd.read_sql(query, self.engine)

        if df.empty:
            raise ValueError("❌ Нет данных в inventory_history (после JOIN с products).")

        # normalize types / datetime
        df['scanned_at'] = pd.to_datetime(df['scanned_at'])
        for col in ['quantity', 'expected_quantity', 'difference', 'min_stock', 'optimal_stock']:
            if col in df.columns:
                df[col] = pd.to_numeric(df[col], errors='coerce')

        df = df.sort_values(['product_id', 'scanned_at']).reset_index(drop=True)

        # Interpolate / fill per product — use transform to keep index alignment
        cols_to_fill = ['quantity', 'expected_quantity', 'difference']
        for col in cols_to_fill:
            if col in df.columns:
                df[col] = df.groupby('product_id')[col].transform(
                    lambda s: s.interpolate(limit_direction='both').ffill().bfill()
                )

        # compute consumption and daily usage
        df['qty_next'] = df.groupby('product_id')['quantity'].shift(-1)
        df['date_next'] = df.groupby('product_id')['scanned_at'].shift(-1)
        df['days_between'] = (df['date_next'] - df['scanned_at']).dt.days
        # avoid zero division
        df.loc[df['days_between'] == 0, 'days_between'] = pd.NA

        df['consumed'] = df['quantity'] - df['qty_next']
        df['daily_usage'] = (df['consumed'] / df['days_between']).abs()

        # Replace infinite or invalid daily usage with NaN
        df['daily_usage'] = df['daily_usage'].replace([pd.NA, float('inf'), float('-inf')], pd.NA)

        # targets
        df['days_until_stockout'] = (df['quantity'] / df['daily_usage']).round()
        df.loc[df['days_until_stockout'] > 365, 'days_until_stockout'] = 365
        df['recommended_order'] = (df['optimal_stock'] - df['quantity']).clip(lower=0)

        # keep only rows with valid daily_usage and finite targets
        df = df.dropna(subset=['daily_usage', 'days_until_stockout'])

        if df.empty:
            raise ValueError('❌ После расчётов не осталось валидных строк (daily_usage отсутствует).')

        features = df[['quantity', 'expected_quantity', 'difference', 'min_stock', 'optimal_stock']].copy()
        targets = df[['days_until_stockout', 'recommended_order']].copy()

        # sklearn strictness: column names must be strings
        features.columns = features.columns.astype(str)
        targets.columns = targets.columns.astype(str)

        return features, targets
