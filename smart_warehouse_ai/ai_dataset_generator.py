import pandas as pd
import numpy as np
import random

def generate_inventory_dataset(n_samples=1000, forecast_days=7, output_path="data/inventory_training_data.csv"):
    categories = ['Роутеры', 'Модемы', 'Коммутаторы', 'Телефоны', 'Кабели']
    data = []

    for _ in range(n_samples):
        category = random.choice(categories)
        current_stock = random.randint(50, 500)
        avg_daily_sales = random.uniform(3, 20)
        min_stock = random.randint(10, 60)
        optimal_stock = random.randint(100, 300)
        seasonal_factor = random.uniform(0.8, 1.3)

        # создаём прогноз по дням
        daily_stocks = []
        stock = current_stock
        for day in range(1, forecast_days + 1):
            stock -= avg_daily_sales * seasonal_factor + np.random.normal(0, 1)
            stock = max(stock, 0)
            daily_stocks.append(round(stock, 2))

        data.append({
            "category": category,
            "current_stock": current_stock,
            "avg_daily_sales": round(avg_daily_sales, 2),
            "min_stock": min_stock,
            "optimal_stock": optimal_stock,
            "seasonal_factor": round(seasonal_factor, 2),
            **{f"predicted_stock_day{i+1}": daily_stocks[i] for i in range(forecast_days)}
        })

    df = pd.DataFrame(data)
    df.to_csv(output_path, sep=";", index=False)
    print(f"✅ Dataset saved as {output_path}")
    print(df.head())

if __name__ == "__main__":
    generate_inventory_dataset(n_samples=1000, forecast_days=7)
