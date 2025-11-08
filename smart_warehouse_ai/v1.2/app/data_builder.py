import pandas as pd
from sqlalchemy import create_engine


class DataBuilder:
    """
    Builds dataset from inventory_history + products + product_warehouse.
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
            pw.min_stock,
            pw.optimal_stock
        FROM inventory_history ih
        JOIN product_warehouse pw ON pw.product_id = ih.product_id AND pw.warehouse_id = ih.warehouse_id
        WHERE ih.is_deleted = false
          AND pw.is_deleted = false
        ORDER BY ih.product_id, ih.scanned_at
        """

        df = pd.read_sql(query, self.engine)

        if df.empty:
            raise ValueError("Нет данных в inventory_history (с JOIN product_warehouse).")

        # normalize types / datetime
        df['scanned_at'] = pd.to_datetime(df['scanned_at'])
        for col in ['quantity', 'expected_quantity', 'difference', 'min_stock', 'optimal_stock']:
            if col in df.columns:
                df[col] = pd.to_numeric(df[col], errors='coerce')

        df = df.sort_values(['product_id', 'scanned_at']).reset_index(drop=True)

        # Interpolate / fill per product
        for col in ['quantity', 'expected_quantity', 'difference']:
            if col in df.columns:
                df[col] = df.groupby('product_id')[col].transform(
                    lambda s: s.interpolate(limit_direction='both').ffill().bfill()
                )

        # compute consumption and daily usage
        df['qty_next'] = df.groupby('product_id')['quantity'].shift(-1)
        df['date_next'] = df.groupby('product_id')['scanned_at'].shift(-1)
        df['days_between'] = (df['date_next'] - df['scanned_at']).dt.days
        df.loc[df['days_between'] == 0, 'days_between'] = pd.NA

        df['consumed'] = df['quantity'] - df['qty_next']
        df['daily_usage'] = (df['consumed'] / df['days_between']).abs()
        df['daily_usage'] = df['daily_usage'].replace([pd.NA, float('inf'), float('-inf')], pd.NA)

        # targets
        df['days_until_stockout'] = (df['quantity'] / df['daily_usage']).round()
        df.loc[df['days_until_stockout'] > 365, 'days_until_stockout'] = 365
        df['recommended_order'] = (df['optimal_stock'] - df['quantity']).clip(lower=0)

        df = df.dropna(subset=['daily_usage', 'days_until_stockout'])

        if df.empty:
            raise ValueError("Недостаточно данных для обучения (daily_usage не рассчитан).")

        features = df[['quantity', 'expected_quantity', 'difference', 'min_stock', 'optimal_stock']].copy()
        targets = df[['days_until_stockout', 'recommended_order']].copy()

        features.columns = features.columns.astype(str)
        targets.columns = targets.columns.astype(str)

        return features, targets