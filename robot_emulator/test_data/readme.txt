## Start
$env:POSTGRES_HOST='localhost'
$env:POSTGRES_USER='warehouse_user' 
$env:POSTGRES_PASSWORD='warehouse_pass'
$env:POSTGRES_DB='smart_warehouse'
$env:ADMIN_ACCESS_TOKEN='<access_token>'

python seed_warehouse.py
