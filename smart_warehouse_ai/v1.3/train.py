"""
Шаг 2 — обучает три квантильные LightGBM-модели на каждый товар.
Запуск:
    python train.py
Модели сохраняются в models/model_p{id}_{lower|median|upper}.joblib
"""

import os
import pickle

import joblib
import numpy as np
import pandas as pd
from lightgbm import LGBMRegressor
from sklearn.metrics import mean_absolute_error, r2_score
from sklearn.model_selection import train_test_split

DATASET = "data/dataset.pkl"
MODEL_DIR = "models"
FEATURES = [
    "quantity",
    "q_lag_1",
    "q_lag_7",
    "q_roll_7",
    "day_of_week",
    "min_stock",
    "optimal_stock",
]
HORIZON = 7  # на сколько дней вперёд учим


def make_xy(df: pd.DataFrame, horizon: int):
    """
    Берёт DataFrame одного товара и создаёт X (фичи) и y (таргет = quantity через h дней).
    Горизонт от 1 до horizon — все сэмплы склеиваются.
    """
    X_parts, y_parts = [], []
    for h in range(1, horizon + 1):
        tmp = df.copy()
        tmp["target"] = tmp["quantity"].shift(-h)
        usable = tmp.dropna(subset=["target"])
        if len(usable) == 0:
            continue
        X_parts.append(usable[FEATURES])
        y_parts.append(usable["target"])
    if not X_parts:
        return None, None
    return pd.concat(X_parts, ignore_index=True), pd.concat(y_parts, ignore_index=True)


def train_one_product(pid: int, df: pd.DataFrame):
    X, y = make_xy(df, HORIZON)
    if X is None or len(X) < 10:
        print(f"  ⚠️ product_id={pid}: недостаточно данных ({len(df)} строк) — пропущен")
        return None

    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42)

    models = {}
    scores = {}

    # Три квантиля через встроенный objective LightGBM
    for name, alpha, obj in [
        ("lower", 0.10, "quantile"),
        ("median", 0.50, "quantile"),
        ("upper", 0.90, "quantile"),
    ]:
        m = LGBMRegressor(
            objective=obj,
            alpha=alpha,
            n_estimators=200,
            random_state=42,
            verbose=-1,  # тихо
        )
        m.fit(X_tr, y_tr)
        pred = m.predict(X_te)
        models[name] = m
        scores[name] = {
            "r2": round(float(r2_score(y_te, pred)), 4),
            "mae": round(float(mean_absolute_error(y_te, pred)), 2),
        }

    # Сохраняем три файла на товар
    for name, mdl in models.items():
        path = os.path.join(MODEL_DIR, f"model_p{pid}_{name}.joblib")
        joblib.dump(mdl, path)

    return scores


if __name__ == "__main__":
    os.makedirs(MODEL_DIR, exist_ok=True)

    with open(DATASET, "rb") as f:
        dataset = pickle.load(f)

    print(f"🤖 Обучаю LightGBM для {len(dataset)} товаров...\n")

    for pid, pdf in dataset.items():
        res = train_one_product(pid, pdf)
        if res:
            print(
                f"  ✅ product_id={pid:>5} | "
                f"low R²={res['lower']['r2']:.3f} MAE={res['lower']['mae']:.1f} | "
                f"med R²={res['median']['r2']:.3f} MAE={res['median']['mae']:.1f} | "
                f"up  R²={res['upper']['r2']:.3f} MAE={res['upper']['mae']:.1f}"
            )

    print(f"\n💾 Модели сохранены в {MODEL_DIR}/")
    print("✅ Готово.")
