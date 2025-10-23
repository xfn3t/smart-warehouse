import pandas as pd
import psycopg2

def load_training_data(conn, horizon_days=7):
    query = f"""
    WITH daily_stock AS (
        SELECT 
            ih.product_id,
            p.category,
            p.min_stock,
            p.optimal_stock,
            DATE_TRUNC('day', ih.scanned_at) AS day,
            SUM(ih.quantity) AS total_stock
        FROM inventory_history ih
        JOIN products p ON ih.product_id = p.id
        GROUP BY ih.product_id, p.category, p.min_stock, p.optimal_stock, DATE_TRUNC('day', ih.scanned_at)
    ),
    sales AS (
        SELECT 
            product_id,
            AVG(LAG(total_stock) OVER (PARTITION BY product_id ORDER BY day) - total_stock) AS avg_daily_sales
        FROM daily_stock
        GROUP BY product_id
    ),
    future_stock AS (
        SELECT 
            ds1.product_id,
            ds1.category,
            ds1.day AS current_day,
            ds1.total_stock AS current_stock,
            s.avg_daily_sales,
            p.min_stock,
            p.optimal_stock,
            ds2.total_stock AS future_stock
        FROM daily_stock ds1
        JOIN sales s ON ds1.product_id = s.product_id
        JOIN products p ON ds1.product_id = p.id
        LEFT JOIN daily_stock ds2 
            ON ds1.product_id = ds2.product_id
           AND ds2.day = ds1.day + INTERVAL '{horizon_days} day'
    )
    SELECT * FROM future_stock WHERE future_stock IS NOT NULL;
    """
    df = pd.read_sql(query, conn)
    return df

if __name__ == "__main__":
    conn = psycopg2.connect("dbname=smart_warehouse user=warehouse_user password=warehouse_pass host=localhost")
    df = load_training_data(conn, horizon_days=7)
    df.to_csv("data/training_from_db_7d.csv", sep=";", index=False)
    print("✅ Training dataset created from database")
    print(df.head())
