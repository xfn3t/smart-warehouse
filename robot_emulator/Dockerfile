FROM python:3.11-slim

WORKDIR /app/backend/emulator


RUN apt-get update && \
    apt-get install -y --no-install-recommends gcc libpq-dev build-essential && \
    rm -rf /var/lib/apt/lists/*


COPY . /app/backend/emulator


COPY requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir -r /app/requirements.txt


ENV PYTHONUNBUFFERED=1
ENTRYPOINT ["python", "emulator.py"]
