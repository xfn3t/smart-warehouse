from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import Optional
from app.model_service import ModelService
from app.data_builder import DataBuilder

router = APIRouter()
model_service = ModelService()


class PredictRequest(BaseModel):
    sku: str | None = Field(None, example="PRD-0022")
    quantity: float = Field(..., example=50.0)
    expected_quantity: float = Field(..., example=60.0)
    difference: float = Field(..., example=-10.0)
    min_stock: float = Field(..., example=20.0)
    optimal_stock: float = Field(..., example=100.0)


class TrainRequest(BaseModel):
    db_url: Optional[str] = None


def classify_critical_level(quantity: float, min_stock: float, optimal_stock: float, days_until_stockout: float) -> str:
    if days_until_stockout < 7 or quantity <= min_stock:
        return "CRITICAL"
    elif days_until_stockout < 30 or quantity <= optimal_stock * 0.5:
        return "MEDIUM"
    return "LOW"


@router.post("/predict")
def predict(req: list[PredictRequest]):
    """
    Accepts list of products with sku and features.
    Returns prediction per product with criticality level.
    """
    try:
        results = []
        for item in req:
            data = item.dict()
            sku = data.pop("sku", None)
            pred = model_service.predict(data)

            # добавляем уровень критичности
            pred["critical_level"] = classify_critical_level(
                quantity=data["quantity"],
                min_stock=data["min_stock"],
                optimal_stock=data["optimal_stock"],
                days_until_stockout=pred["days_until_stockout"]
            )

            pred["sku"] = sku
            results.append(pred)

        return {"status": "ok", "prediction": results}

    except FileNotFoundError:
        raise HTTPException(status_code=404, detail="Модель не найдена. Сначала обучите её через /train.")
    except KeyError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))