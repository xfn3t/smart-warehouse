Stop-Service -Name postgresql-x64-17 -Force


DROP SCHEMA IF EXISTS public CASCADE;
CREATE SCHEMA public;

## Before start
ALTER TABLE location
ADD CONSTRAINT uq_location_coords UNIQUE (warehouse_id, zone, row, shelf);

## Start
$env:POSTGRES_HOST='localhost'
$env:POSTGRES_USER='warehouse_user' 
$env:POSTGRES_PASSWORD='warehouse_pass'
$env:POSTGRES_DB='smart_warehouse'
$env:ADMIN_ACCESS_TOKEN='<access_token>'

python seed_warehouse.py