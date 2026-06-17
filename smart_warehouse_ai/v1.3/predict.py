"""
Шаг 3 — загружает обученные модели и прогнозирует остаток на 7 дней вперёд.
Запуск:
    python predict.py
Или импорт:
    from predict import predict_week
    result = predict_week(product_id=1, dataset=pickle.load(open("data/dataset.pkl","rb")))
"""

import os
import pickle

import joblib
import numpy as np
import pandas as pd

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


def predict_week(product_id: int, dataset: dict[int, pd.DataFrame]) -> list[dict]:
    """
    Принимает product_id и словарь {product_id: DataFrame} (как из build_dataset).
    Возвращает список из 7 dict'ов:
        {date, lower, median, upper, confidence, criticality}
    """
    # 1. Загружаем модели
    models = {}
    for name in ("lower", "median", "upper"):
        path = os.path.join(MODEL_DIR, f"model_p{product_id}_{name}.joblib")
        if not os.path.exists(path):
            raise FileNotFoundError(
                f"Модель {path} не найдена. Сначала запусти train.py для product_id={product_id}"
            )
        models[name] = joblib.load(path)

    # 2. Берём историю товара
    if product_id not in dataset:
        raise KeyError(
            f"product_id={product_id} отсутствует в dataset. Проверь build_dataset."
        )

    hist = dataset[product_id].copy()
    min_stock = float(hist["min_stock"].iloc[-1])
    opt_stock = float(hist["optimal_stock"].iloc[-1])

    # 3. Рекурсивное предсказание на 7 дней
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

        # confidence = насколько узок коридор относительно медианы
        width = upper - lower
        conf = round(float(1.0 - min(1.0, width / max(median, 1.0))), 3)

        # criticality
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

        # Добавляем предсказанный день как новую строку истории
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


# ── CLI ──────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    import sys

    with open("data/dataset.pkl", "rb") as f:
        dataset = pickle.load(f)

    # Если передали product_id через командную строку
    if len(sys.argv) > 1:
        pids = [int(sys.argv[1])]
    else:
        pids = list(dataset.keys())

    for pid in pids:
        print(f"\n📦 product_id={pid}")
        try:
            week = predict_week(pid, dataset)
            for d in week:
                print(
                    f"  {d['date']} | med={d['median']:>7.1f} | "
                    f"[{d['lower']:.0f} … {d['upper']:.0f}] | "
                    f"conf={d['confidence']:.2f} | {d['criticality']}"
                )
        except (FileNotFoundError, KeyError) as e:
            print(f"  ⚠️ {e}")
