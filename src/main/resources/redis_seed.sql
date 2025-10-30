-- 0) Починить последовательность/identity для inventory_history.id
DO $$
DECLARE
  seq_name text;
BEGIN
  -- попробуем найти sequence, связанный с колонкой id
  SELECT pg_get_serial_sequence('inventory_history','id') INTO seq_name;

  IF seq_name IS NOT NULL THEN
    -- выставляем last value = MAX(id), чтобы nextval() дал max+1
    PERFORM setval(seq_name, COALESCE((SELECT MAX(id) FROM inventory_history),0), true);
  ELSE
    -- если serial/sequence не найден (identity-колонка) — делаем RESTART WITH max+1
    EXECUTE format(
      'ALTER TABLE %I ALTER COLUMN %I RESTART WITH %s',
      'inventory_history','id',
      COALESCE((SELECT (MAX(id)+1)::text FROM inventory_history), '1')
    );
  END IF;
END $$;

-- 1) Гарантируем таблицу статусов и коды
CREATE TABLE IF NOT EXISTS inventory_statuses (
  id   BIGSERIAL PRIMARY KEY,
  code VARCHAR(64) UNIQUE NOT NULL,
  name VARCHAR(255) NOT NULL
);

INSERT INTO inventory_statuses(code,name) VALUES
  ('OK','В норме'),
  ('LOW_STOCK','Недостаток'),
  ('CRITICAL','Критично')
ON CONFLICT (code) DO NOTHING;

-- 2) Набросать ~120 строк за последний час (локальное время сервера), 2 записи/минуту
CREATE EXTENSION IF NOT EXISTS pgcrypto;

WITH base AS (
  SELECT (now())::timestamp AS now_local
),
m AS (
  SELECT gs::int AS minute_off FROM generate_series(0,59) gs
),
batch1 AS (
  SELECT
    gen_random_uuid()                                          AS message_id,
    (SELECT id FROM warehouses ORDER BY random() LIMIT 1)      AS warehouse_id,
    (SELECT id FROM robots    ORDER BY random() LIMIT 1)       AS robot_id,
    (SELECT id FROM products  ORDER BY random() LIMIT 1)       AS product_id,
    (1 + floor(random()*120))::int                             AS zone,
    (1 + floor(random()*200))::int                             AS row_number,
    (1 + floor(random()*20))::int                              AS shelf_number,
    (10 + floor(random()*40))::int                             AS expected_quantity,
    GREATEST(0, (10 + floor(random()*40))::int + (-6 + floor(random()*13))::int)::int AS quantity,
    (b.now_local - (m.minute_off * interval '1 minute'))       AS scanned_at,
    (b.now_local - (m.minute_off * interval '1 minute')) + ((1 + floor(random()*9)) * interval '1 minute') AS created_at
  FROM base b, m
),
batch2 AS (
  SELECT
    gen_random_uuid()                                          AS message_id,
    (SELECT id FROM warehouses ORDER BY random() LIMIT 1)      AS warehouse_id,
    (SELECT id FROM robots    ORDER BY random() LIMIT 1)       AS robot_id,
    (SELECT id FROM products  ORDER BY random() LIMIT 1)       AS product_id,
    (1 + floor(random()*120))::int                             AS zone,
    (1 + floor(random()*200))::int                             AS row_number,
    (1 + floor(random()*20))::int                              AS shelf_number,
    (5 + floor(random()*50))::int                              AS expected_quantity,
    GREATEST(0, (5 + floor(random()*50))::int + (-4 + floor(random()*9))::int)::int  AS quantity,
    (b.now_local - (m.minute_off * interval '1 minute'))       AS scanned_at,
    (b.now_local - (m.minute_off * interval '1 minute')) + ((2 + floor(random()*9)) * interval '1 minute') AS created_at
  FROM base b, m
),
all_rows AS (
  SELECT * FROM batch1
  UNION ALL
  SELECT * FROM batch2
),
with_calc AS (
  SELECT
    r.*,
    (r.quantity - r.expected_quantity) AS difference,
    CASE
      WHEN (r.quantity = 0 AND r.expected_quantity > 0) OR (r.quantity - r.expected_quantity) <= -10 THEN 'CRITICAL'
      WHEN (r.quantity - r.expected_quantity) BETWEEN -9 AND -1 THEN 'LOW_STOCK'
      ELSE 'OK'
    END AS status_code
  FROM all_rows r
)
INSERT INTO inventory_history (
  message_id, warehouse_id, robot_id, product_id,
  zone, row_number, shelf_number,
  expected_quantity, quantity, difference,
  status_id, scanned_at, created_at, is_deleted
)
SELECT
  w.message_id, w.warehouse_id, w.robot_id, w.product_id,
  w.zone, w.row_number, w.shelf_number,
  w.expected_quantity, w.quantity, w.difference,
  s.id::bigint AS status_id,
  w.scanned_at,
  GREATEST(w.created_at, w.scanned_at + interval '1 minute') AS created_at,
  FALSE
FROM with_calc w
JOIN inventory_statuses s ON s.code = w.status_code;
