"""
ModelService: загрузка/обучение/предсказание LightGBM квантильных моделей.
"""

import os

import joblib
import numpy as np
import pandas as pd
from lightgbm import LGBMRegressor
from sklearn.metrics import mean_absolute_error, r2_score
from sklearn.model_selection import train_test_split

MODEL_DIR = os.getenv("MODEL_DIR", "models")
FEATURES = [
    "quantity",
    "q_lag_1",
    "q_lag_7",
    "q_roll_7",
    "day_of_week",
    "min_stock",
    "optimal_stock",
]
HORIZON = 7


class ModelService:
    def _make_xy(self, df: pd.DataFrame):
        X_parts, y_parts = [], []
        for h in range(1, HORIZON + 1):
            tmp = df.copy()
            tmp["target"] = tmp["quantity"].shift(-h)
            usable = tmp.dropna(subset=["target"])
            if len(usable) == 0:
                continue
            X_parts.append(usable[FEATURES])
            y_parts.append(usable["target"])
        if not X_parts:
            return None, None
        return pd.concat(X_parts, ignore_index=True), pd.concat(
            y_parts, ignore_index=True
        )

    # ── обучение всех товаров ────────────────────────────────────────────

    def train_all(self, dataset: dict[int, pd.DataFrame]) -> int:
        os.makedirs(MODEL_DIR, exist_ok=True)
        trained = 0
        for pid, pdf in dataset.items():
            if self._train_one(pid, pdf):
                trained += 1
        return trained

    def _train_one(self, pid: int, df: pd.DataFrame) -> bool:
        X, y = self._make_xy(df)
        if X is None or len(X) < 10:
            return False

        X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42)

        for name, alpha in [("lower", 0.10), ("median", 0.50), ("upper", 0.90)]:
            m = LGBMRegressor(
                objective="quantile",
                alpha=alpha,
                n_estimators=200,
                random_state=42,
                verbose=-1,
            )
            m.fit(X_tr, y_tr)
            path = os.path.join(MODEL_DIR, f"model_p{pid}_{name}.joblib")
            joblib.dump(m, path)

        return True

    # ── предсказание на 7 дней ───────────────────────────────────────────

    def predict_week(
        self, product_id: int, dataset: dict[int, pd.DataFrame]
    ) -> list[dict]:
        # загружаем 3 модели
        models = {}
        for name in ("lower", "median", "upper"):
            path = os.path.join(MODEL_DIR, f"model_p{product_id}_{name}.joblib")
            if not os.path.exists(path):
                raise FileNotFoundError(
                    f"Модель {path} не найдена. Обучите через /train."
                )
            models[name] = joblib.load(path)

        if product_id not in dataset:
            raise KeyError(f"product_id={product_id} отсутствует в датасете.")

        hist = dataset[product_id].copy()
        min_stock = float(hist["min_stock"].iloc[-1])
        opt_stock = float(hist["optimal_stock"].iloc[-1])

        preds = []
        df = hist.copy()

        for _ in range(7):
            last = df.iloc[-1]
            X = pd.DataFrame(
                [
                    {
                        "quantity": last["quantity"],
                        "q_lag_1": last["quantity"]
                        if len(df) < 2
                        else df["quantity"].iloc[-2],
                        "q_lag_7": last["quantity"]
                        if len(df) < 8
                        else df["quantity"].iloc[-8],
                        "q_roll_7": float(df["quantity"].tail(7).mean()),
                        "day_of_week": (last["day_of_week"] + 1) % 7,
                        "min_stock": last["min_stock"],
                        "optimal_stock": last["optimal_stock"],
                    }
                ]
            )

            lower = max(0.0, float(models["lower"].predict(X)[0]))
            median = max(0.0, float(models["median"].predict(X)[0]))
            upper = max(0.0, float(models["upper"].predict(X)[0]))

            next_date = pd.to_datetime(last["scanned_at"]) + pd.Timedelta(days=1)

            width = upper - lower
            conf = round(float(1.0 - min(1.0, width / max(median, 1.0))), 3)

            if median <= min_stock:
                crit = "CRITICAL"
            elif median <= opt_stock * 0.5:
                crit = "LOW"
            else:
                crit = "OK"

            preds.append(
                {
                    "date": next_date.date().isoformat(),
                    "lower": lower,
                    "median": median,
                    "upper": upper,
                    "confidence": conf,
                    "criticality": crit,
                }
            )

            # добавляем предсказанный день в историю для следующей итерации
            new_row = {
                "scanned_at": next_date,
                "quantity": median,
                "q_lag_1": last["quantity"],
                "q_lag_7": df["quantity"].iloc[-7]
                if len(df) >= 7
                else df["quantity"].iloc[0],
                "q_roll_7": float(
                    pd.concat([df["quantity"].tail(6), pd.Series([median])]).mean()
                ),
                "day_of_week": next_date.dayofweek,
                "min_stock": last["min_stock"],
                "optimal_stock": last["optimal_stock"],
            }
            df = pd.concat([df, pd.DataFrame([new_row])], ignore_index=True)

        return preds
