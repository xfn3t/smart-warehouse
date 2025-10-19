import pandas as pd
import numpy as np
import random

def generate_inventory_dataset(n_samples=1000):
    categories = ['Роутеры', 'Модемы', 'Коммутаторы', 'Телефоны', 'Кабели']
    data = []
    
    for _ in range(n_samples):
        category = random.choice(categories)
        current_stock = random.randint(10, 300)
        avg_daily_sales = random.uniform(1, 15)
        min_stock = random.randint(10, 50)
        optimal_stock = random.randint(80, 200)
        seasonal_factor = random.uniform(0.8, 1.2)
        
        # прогноз остатка
        predicted_stock_7d = current_stock - avg_daily_sales * 7 * seasonal_factor
        predicted_stock_7d += np.random.normal(0, 5)  # шум
        predicted_stock_7d = max(predicted_stock_7d, 0)
        
        if predicted_stock_7d <= min_stock:
            status = "CRITICAL"
        elif predicted_stock_7d <= min_stock * 1.5:
            status = "LOW"
        else:
            status = "OK"
        
        data.append({
            "category": category,
            "current_stock": current_stock,
            "avg_daily_sales": round(avg_daily_sales, 2),
            "min_stock": min_stock,
            "optimal_stock": optimal_stock,
            "seasonal_factor": round(seasonal_factor, 2),
            "predicted_stock_7d": round(predicted_stock_7d, 2),
            "status": status
        })
    
    df = pd.DataFrame(data)
    df.to_csv("inventory_training_data.csv", sep=";", index=False)
    print("✅ Dataset saved as inventory_training_data.csv")
    print(df.head())

if __name__ == "__main__":
    generate_inventory_dataset(1000)
