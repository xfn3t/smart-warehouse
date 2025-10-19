from fastapi import FastAPI
from pydantic import BaseModel
import pandas as pd
import joblib

app = FastAPI(title="Smart Warehouse AI Predictor")

model = joblib.load("inventory_predictor.pkl")

class ProductData(BaseModel):
    category: str
    current_stock: int
    avg_daily_sales: float
    min_stock: int
    optimal_stock: int
    seasonal_factor: float

@app.post("/api/ai/predict")
def predict_stock(data: list[ProductData]):
    df = pd.DataFrame([d.dict() for d in data])
    predictions = model.predict(df)
    
    result = []
    for i, row in enumerate(df.to_dict(orient="records")):
        result.append({
            **row,
            "predicted_stock_7d": round(predictions[i], 2),
            "days_until_stockout": int(row["current_stock"] / (row["avg_daily_sales"] * row["seasonal_factor"])) if row["avg_daily_sales"] > 0 else None,
            "recommended_order": max(0, row["optimal_stock"] - predictions[i]),
        })
    return {"predictions": result}
