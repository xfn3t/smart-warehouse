from app.api.routes import router
from fastapi import FastAPI

app = FastAPI(title="Smart Warehouse AI v1.3")
app.include_router(router, prefix="/api")


@app.get("/")
def root():
    return {"status": "ok", "service": "Smart Warehouse AI", "version": "1.3"}
