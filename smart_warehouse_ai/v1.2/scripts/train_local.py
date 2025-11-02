import sys, os
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.data_builder import DataBuilder
from app.model_service import ModelService

if __name__ == "__main__":
    DB_URL = os.getenv("DATABASE_URL", "postgresql+psycopg2://warehouse_user:warehouse_pass@postgres:5432/smart_warehouse")
    print("📊 Building dataset...")
    builder = DataBuilder(DB_URL)
    X, y = builder.build_dataset()

    print("🤖 Training model...")
    svc = ModelService()
    result = svc.train(X, y)
    print("Done:", result)
