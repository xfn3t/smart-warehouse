"""
Собирает признаки с лагами для каждого товара из базы.
Используется из API (/train) и из CLI (build_dataset.py).
"""

import pandas as pd
from sqlalchemy import create_engine


def build_dataset(db_url: str) -> dict[int, pd.DataFrame]:
    """
    Возвращает словарь: product_id → DataFrame с признаками.
    Каждый DataFrame содержит историю по одному товару с лаговыми фичами.
    """
    engine = create_engine(db_url)

    df = pd.read_sql(
        """
        SELECT
            ih.product_id,
            ih.quantity,
            pw.min_stock,
            pw.optimal_stock,
            ih.scanned_at
        FROM inventory_history ih
        JOIN product_warehouse pw
          ON pw.product_id = ih.product_id
         AND pw.warehouse_id = ih.warehouse_id
        WHERE ih.is_deleted = false
          AND pw.is_deleted = false
        ORDER BY ih.product_id, ih.scanned_at
    """,
        engine,
    )
    engine.dispose()

    if df.empty:
        raise ValueError(
            "Нет данных для обучения. Проверь inventory_history и product_warehouse."
        )

    df["scanned_at"] = pd.to_datetime(df["scanned_at"])
    for col in ["quantity", "min_stock", "optimal_stock"]:
        df[col] = pd.to_numeric(df[col], errors="coerce")

    df = df.sort_values(["product_id", "scanned_at"]).reset_index(drop=True)
    df = df.drop_duplicates(subset=["product_id", "scanned_at"], keep="last")

    results: dict[int, pd.DataFrame] = {}

    for pid, g in df.groupby("product_id"):
        g = g.sort_values("scanned_at").reset_index(drop=True)
        g["quantity"] = (
            g["quantity"].interpolate(limit_direction="both").ffill().bfill()
        )

        g["q_lag_1"] = g["quantity"].shift(1).fillna(g["quantity"])
        g["q_lag_7"] = g["quantity"].shift(7).fillna(g["quantity"])
        g["q_roll_7"] = (
            g["quantity"].rolling(7, min_periods=1).mean().fillna(g["quantity"])
        )
        g["day_of_week"] = g["scanned_at"].dt.dayofweek

        g = g[
            [
                "scanned_at",
                "quantity",
                "q_lag_1",
                "q_lag_7",
                "q_roll_7",
                "day_of_week",
                "min_stock",
                "optimal_stock",
            ]
        ]

        if len(g) >= 14:
            results[int(pid)] = g.reset_index(drop=True)

    if not results:
        raise ValueError("Ни по одному товару нет >= 14 строк истории.")

    return results
