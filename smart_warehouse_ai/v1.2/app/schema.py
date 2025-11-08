from sqlalchemy import Table, MetaData, Column, Integer, BigInteger, String, Boolean, Date, TIMESTAMP, Text, ForeignKey, DECIMAL

metadata = MetaData()

products = Table(
    'products', metadata,
    Column('id', BigInteger, primary_key=True),
    Column('sku_code', String(50)),
    Column('name', String(255)),
    Column('category', String(100)),
    Column('is_deleted', Boolean)
)

inventory_history = Table(
    'inventory_history', metadata,
    Column('id', BigInteger, primary_key=True),
    Column('message_id', String),
    Column('warehouse_id', BigInteger),
    Column('robot_id', BigInteger),
    Column('product_id', BigInteger),
    Column('location_id', BigInteger),
    Column('expected_quantity', Integer),
    Column('quantity', Integer),
    Column('difference', Integer),
    Column('status_id', BigInteger),
    Column('scanned_at', TIMESTAMP),
    Column('created_at', TIMESTAMP),
    Column('is_deleted', Boolean)
)

product_warehouse = Table(
    'product_warehouse', metadata,
    Column('id', BigInteger, primary_key=True),
    Column('product_id', BigInteger),
    Column('warehouse_id', BigInteger),
    Column('min_stock', Integer),
    Column('optimal_stock', Integer),
    Column('created_at', TIMESTAMP),
    Column('is_deleted', Boolean)
)