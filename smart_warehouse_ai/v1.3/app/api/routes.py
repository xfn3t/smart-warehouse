"""
REST API:
  POST /api/predict       — старый контракт: 5 фичей → days_until_stockout + recommended_order
  POST /api/predict/week  — новый: product_id → 7 дней прогноза с квантилями
  POST /api/train         — переобучить модель из базы
"""

import pickle
from pathlib import Path
from typing import Optional

import pandas as pd
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from app.data_builder import build_dataset
from app.model_service import ModelService

router = APIRouter()

# ── синглтон сервиса ─────────────────────────────────────────────────────
model_service = ModelService()
_dataset_cache: Optional[dict[int, pd.DataFrame]] = None

DATASET_PATH = Path("data/dataset.pkl")


def _get_dataset() -> dict[int, pd.DataFrame]:
    global _dataset_cache
    if _dataset_cache is not None:
        return _dataset_cache
    if not DATASET_PATH.exists():
        raise HTTPException(
            status_code=404,
            detail="Датасет не найден. Запустите build_dataset.py или /train.",
        )
    with open(DATASET_PATH, "rb") as f:
        _dataset_cache = pickle.load(f)
    return _dataset_cache


# ── схемы ─────────────────────────────────────────────────────────────────


class PredictRequest(BaseModel):
    sku: Optional[str] = Field(None, example="PRD-0022")
    quantity: float = Field(..., example=50.0)
    expected_quantity: float = Field(..., example=60.0)
    difference: float = Field(..., example=-10.0)
    min_stock: float = Field(..., example=20.0)
    optimal_stock: float = Field(..., example=100.0)


class PredictWeekRequest(BaseModel):
    product_id: int = Field(..., example=1)


class TrainRequest(BaseModel):
    db_url: Optional[str] = None


# ── хелперы ───────────────────────────────────────────────────────────────


def _classify(
    quantity: float, min_stock: float, optimal_stock: float, days: float
) -> str:
    if days < 7:
        return "CRITICAL"
    elif days < 30:
        return "MEDIUM"
    return "OK"


# ══════════════════════════════════════════════════════════════════════════
# ENDPOINTS
# ══════════════════════════════════════════════════════════════════════════


@router.post("/predict")
def predict(req: list[PredictRequest]):
    """
    Старый контракт v1.2: список товаров с 5 фичами.
    Возвращает days_until_stockout и recommended_order на основе daily_usage.
    """
    try:
        results = []
        for item in req:
            data = item.model_dump()
            sku = data.pop("sku", None)

            # считаем daily_usage = expected - quantity (примерно)
            daily_usage = max(abs(data["difference"]), 0.01)
            days = round(data["quantity"] / daily_usage, 1)
            if days > 365:
                days = 365.0
            order = max(0.0, round(data["optimal_stock"] - data["quantity"], 1))

            results.append(
                {
                    "sku": sku,
                    "days_until_stockout": days,
                    "recommended_order": order,
                    "critical_level": _classify(
                        data["quantity"], data["min_stock"], data["optimal_stock"], days
                    ),
                }
            )

        return {"status": "ok", "prediction": results}

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/predict/week")
def predict_week(req: PredictWeekRequest):
    """
    Новый контракт: product_id → 7 дней прогноза с квантилями и confidence.
    """
    try:
        dataset = _get_dataset()
        result = model_service.predict_week(req.product_id, dataset)
        return {"status": "ok", "product_id": req.product_id, "forecast": result}
    except FileNotFoundError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except KeyError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/train")
def train(req: TrainRequest = None):
    """
    Переобучает модель из базы: собирает датасет заново и обучает все товары.
    """
    import os

    from dotenv import load_dotenv

    load_dotenv()

    db_url = (req.db_url if req and req.db_url else None) or os.getenv(
        "DATABASE_URL",
        "postgresql+psycopg2://warehouse_user:warehouse_pass@postgres:5432/smart_warehouse",
    )

    try:
        global _dataset_cache
        _dataset_cache = build_dataset(db_url)

        DATASET_PATH.parent.mkdir(parents=True, exist_ok=True)
        with open(DATASET_PATH, "wb") as f:
            pickle.dump(_dataset_cache, f)

        trained = model_service.train_all(_dataset_cache)

        return {
            "status": "ok",
            "products_trained": trained,
            "total_products": len(_dataset_cache),
        }

    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
