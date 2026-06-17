FROM python:3.11-slim

WORKDIR /app

COPY v1.3/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY v1.3/ .

EXPOSE 8001

CMD ["sh", "-c", "python build_dataset.py && python train.py && uvicorn app.main:app --host 0.0.0.0 --port 8001"]
