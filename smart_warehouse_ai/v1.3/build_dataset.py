"""
Шаг 1 — CLI: достаёт историю из базы и сохраняет data/dataset.pkl.
Запуск:
    python build_dataset.py
"""

import os
import pickle

from app.data_builder import build_dataset
from dotenv import load_dotenv

load_dotenv()
DB_URL = os.getenv(
    "DATABASE_URL",
    "postgresql+psycopg2://warehouse_user:warehouse_pass@localhost:5432/smart_warehouse",
)
OUTPUT = "data/dataset.pkl"

if __name__ == "__main__":
    os.makedirs("data", exist_ok=True)
    ds = build_dataset(DB_URL)
    with open(OUTPUT, "wb") as f:
        pickle.dump(ds, f)
    print(f"✅ Собрано товаров: {len(ds)}")
    print(f"💾 Сохранено в {OUTPUT}")
    for pid, pdf in list(ds.items())[:3]:
        print(f"  product_id={pid}: {len(pdf)} строк")
