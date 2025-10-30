import os
from typing import Dict, Any
import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import r2_score, mean_absolute_error

MODEL_DIR = os.getenv('MODEL_DIR', 'models')
os.makedirs(MODEL_DIR, exist_ok=True)


class ForecastModel:
    """Wrapper: trains 3 quantile models (lower, median, upper) using GradientBoostingRegressor.
    Predicts 7 days ahead by recursively using predicted values as lag features.
    """

    def __init__(self):
        # models per quantile
        self.models = {
            'lower': GradientBoostingRegressor(loss='quantile', alpha=0.1, n_estimators=200),
            'median': GradientBoostingRegressor(loss='ls', n_estimators=200),
            'upper': GradientBoostingRegressor(loss='quantile', alpha=0.9, n_estimators=200),
        }

    def _features_targets(self, df: pd.DataFrame, horizon=7):
        # df expected to have columns: date, quantity, q_lag_1, q_lag_7, q_roll_7, day_of_week, product_id, min_stock, optimal_stock
        X_rows = []
        y_rows = []
        for h in range(1, horizon+1):
            tmp = df.copy()
            tmp['target'] = tmp['quantity'].shift(-h)
            use = tmp.dropna(subset=['target'])
            if use.empty:
                continue
            features = use[['quantity','q_lag_1','q_lag_7','q_roll_7','day_of_week','min_stock','optimal_stock']]
            X_rows.append(features)
            y_rows.append(use['target'])
        if not X_rows:
            return pd.DataFrame(), pd.Series()
        X = pd.concat(X_rows, ignore_index=True)
        y = pd.concat(y_rows, ignore_index=True)
        return X, y

    def train_for_product(self, df: pd.DataFrame):
        X, y = self._features_targets(df)
        if X.empty:
            raise ValueError('Not enough data')
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        results = {}
        for name, model in self.models.items():
            m = model
            m.fit(X_train, y_train)
            y_pred = m.predict(X_test)
            results[name] = {
                'r2': float(r2_score(y_test, y_pred)),
                'mae': float(mean_absolute_error(y_test, y_pred))
            }
            # persist trained model per product will be done externally
        return results, {k: v for k,v in zip(self.models.keys(), self.models)}

    def save(self, product_id: int, trained_models: Dict[str, Any]):
        for name, model in trained_models.items():
            path = os.path.join(MODEL_DIR, f'model_p{product_id}_{name}.joblib')
            joblib.dump(model, path)

    def load(self, product_id: int):
        loaded = {}
        for name in ['lower','median','upper']:
            path = os.path.join(MODEL_DIR, f'model_p{product_id}_{name}.joblib')
            if not os.path.exists(path):
                return None
            loaded[name] = joblib.load(path)
        return loaded

    def predict_week(self, product_id: int, recent_df: pd.DataFrame):
        # recent_df must contain most recent rows with features
        models = self.load(product_id)
        if models is None:
            raise FileNotFoundError('Model not trained for product')
        preds = []
        df = recent_df.copy().reset_index(drop=True)
        # recursively predict 7 days
        for day in range(1,8):
            feat = df.iloc[-1:][['quantity','q_lag_1','q_lag_7','q_roll_7','day_of_week','min_stock','optimal_stock']]
            # ensure shapes
            X = feat.fillna(0)
            lower = models['lower'].predict(X)[0]
            median = models['median'].predict(X)[0]
            upper = models['upper'].predict(X)[0]
            date = (pd.to_datetime(df['date'].iloc[-1]) + pd.Timedelta(days=1)).normalize()
            preds.append({'date': date.date(), 'lower': float(max(0, lower)), 'median': float(max(0, median)), 'upper': float(max(0, upper))})
            # append predicted median as next day's quantity and update rolling features
            next_row = {
                'date': date,
                'quantity': median,
                'q_lag_1': df['quantity'].iloc[-1],
                'q_lag_7': df['quantity'].shift(1).iloc[-1] if len(df) > 1 else df['quantity'].iloc[-1],
                'q_roll_7': float(pd.concat([df['quantity'], pd.Series([median])]).rolling(7, min_periods=1).mean().iloc[-1]),
                'day_of_week': date.weekday(),
                'min_stock': df['min_stock'].iloc[-1],
                'optimal_stock': df['optimal_stock'].iloc[-1]
            }
            df = pd.concat([df, pd.DataFrame([next_row])], ignore_index=True)
        # compute confidence score
        for p in preds:
            width = p['upper'] - p['lower']
            denom = max(p['median'], 1.0)
            conf = 1.0 - min(1.0, width/denom)
            p['confidence'] = round(float(conf), 3)
            # criticality classification vs min_stock
            p['criticality'] = 'CRITICAL' if p['median'] <= df['min_stock'].iloc[-1] else ('LOW' if p['median'] <= df['optimal_stock'].iloc[-1]*0.5 else 'OK')
        return preds