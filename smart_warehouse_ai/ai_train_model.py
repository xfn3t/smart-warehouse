import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestRegressor
from sklearn.preprocessing import OneHotEncoder
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
import joblib

df = pd.read_csv("training_dataset.csv", sep=";")

X = df[["category", "current_stock", "avg_daily_sales", "min_stock", "optimal_stock", "seasonal_factor"]]
y = df["predicted_stock_7d"]

categorical_features = ["category"]
numeric_features = [c for c in X.columns if c not in categorical_features]

preprocessor = ColumnTransformer([
    ("cat", OneHotEncoder(handle_unknown="ignore"), categorical_features)
], remainder="passthrough")

model = Pipeline([
    ("preprocessor", preprocessor),
    ("regressor", RandomForestRegressor(n_estimators=200, random_state=42))
])

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
model.fit(X_train, y_train)

score = model.score(X_test, y_test)
print(f"✅ Model trained. R² score = {score:.3f}")

joblib.dump(model, "inventory_predictor.pkl")
print("💾 Model saved as inventory_predictor.pkl")
