FROM python:3.11-slim

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends libgomp1 && rm -rf /var/lib/apt/lists/*

COPY v1.3/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY v1.3/ .

EXPOSE 8001

CMD ["sh", "-c", "python build_dataset.py && python train.py && uvicorn app.main:app --host 0.0.0.0 --port 8001"]
