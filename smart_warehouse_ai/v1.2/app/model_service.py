import os
import numpy as np
import joblib
from sklearn.multioutput import MultiOutputRegressor
from sklearn.ensemble import RandomForestRegressor


class ModelService:
    """
    Multi-output model wrapper.
    Feature order expected: [quantity, expected_quantity, difference, min_stock, optimal_stock]
    Output order: [days_until_stockout, recommended_order]
    """

    def __init__(self, model_path: str = "models/inventory_model.pkl"):
        self.model_path = model_path
        self.model = None

    def train(self, X, y):
        # basic validation
        try:
            n_rows = len(X)
        except Exception:
            raise ValueError("X must be array-like or DataFrame")

        if n_rows < 10:
            raise ValueError("❌ Недостаточно данных для обучения: нужно >= 10 строк")

        X_np = X.values if hasattr(X, "values") else np.asarray(X)
        y_np = y.values if hasattr(y, "values") else np.asarray(y)

        base = RandomForestRegressor(n_estimators=200, random_state=42, n_jobs=-1)
        model = MultiOutputRegressor(base)
        model.fit(X_np, y_np)

        os.makedirs(os.path.dirname(self.model_path) or ".", exist_ok=True)
        joblib.dump(model, self.model_path)
        self.model = model
        return {"trained_rows": int(n_rows)}

    def load(self):
        if self.model is None:
            if not os.path.exists(self.model_path):
                raise FileNotFoundError("Модель не найдена: сначала вызовите train")
            self.model = joblib.load(self.model_path)
        return self.model

    def predict(self, X):
        mdl = self.load()

        # accept dict or array-like
        if isinstance(X, dict):
            order = ["quantity", "expected_quantity", "difference", "min_stock", "optimal_stock"]
            try:
                feat = np.array([[float(X[k]) for k in order]])
            except KeyError as e:
                raise KeyError(f"Отсутствует поле: {e}")
        else:
            arr = np.asarray(X, dtype=float)
            feat = arr.reshape(1, -1) if arr.ndim == 1 else arr

        preds = mdl.predict(feat)
        if preds.shape[0] == 1:
            return {"days_until_stockout": float(preds[0, 0]), "recommended_order": float(preds[0, 1])}
        else:
            return [{"days_until_stockout": float(r[0]), "recommended_order": float(r[1])} for r in preds]
