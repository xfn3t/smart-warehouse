from fastapi import FastAPI
from app.api.routes import router

app = FastAPI(title="Smart Warehouse AI")
app.include_router(router, prefix="/api")


@app.get("/")
def root():
    return {"status": "ok", "service": "Smart Warehouse AI"}
