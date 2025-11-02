$env:API_URL="http://localhost:8080"
$env:MODE="live"
$env:POSTGRES_HOST="localhost"
$env:POSTGRES_PORT="5432"
$env:POSTGRES_DB="smart_warehouse"
$env:POSTGRES_USER="warehouse_user"
$env:POSTGRES_PASSWORD="warehouse_pass"
$env:ROBOTS_COUNT="4"

# Запускаем
python .\emulator.py