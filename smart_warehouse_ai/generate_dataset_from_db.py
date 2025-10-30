import psycopg2
import pandas as pd
import numpy as np

# Подключение к PostgreSQL
conn = psycopg2.connect(
    dbname="smart_warehouse",
    user="warehouse_user",
    password="warehouse_pass",
    host="localhost",
    port="5432"
)

# SQL-запрос к inventory_history + products
query = """
SELECT 
    p.id AS product_id,
    p.category,
    ih.quantity AS current_stock,
    (ih.expected_quantity - ih.quantity) AS avg_daily_sales,
    p.min_stock,
    p.optimal_stock,
    random() * 0.4 + 0.8 AS seasonal_factor
FROM inventory_history ih
JOIN products p ON ih.product_id = p.id
WHERE ih.is_deleted = false AND p.is_deleted = false;
"""

# Загружаем данные
df = pd.read_sql_query(query, conn)

if df.empty:
    print("⚠️ No data found in database! Check table names and content.")
else:
    print(f"✅ Loaded {len(df)} rows from database.")

# Прогнозируем остаток на неделю
df["predicted_stock_7d"] = (
    df["current_stock"] - df["avg_daily_sales"] * 7 * df["seasonal_factor"]
).round(2)

# Обработка возможных отрицательных остатков
df["predicted_stock_7d"] = df["predicted_stock_7d"].apply(lambda x: max(x, 0))

# Выводим и сохраняем
print("✅ Training dataset created from database")
print(df.head())

output_path = "training_dataset.csv"
df.to_csv(output_path, index=False, sep=';')
print(f"💾 Saved to {output_path}")

conn.close()
