-- ============================================================================
-- Seed-скрипт: благоприятные данные для v1.3 на складе W-001
-- ~60 дней истории инвентаризации на каждый продукт из inventory_wh01.csv
-- ============================================================================

BEGIN;

-- 0. Найти или создать склад W-001, очистить старые данные ──────────────────
DO $$
DECLARE
    w_id bigint;
BEGIN
    SELECT id INTO w_id FROM warehouses WHERE code = 'W-001' AND is_deleted = false;
    IF NOT FOUND THEN
        INSERT INTO warehouses (code, zone_max_size, row_max_size, shelf_max_size, name, location, is_deleted)
        VALUES ('W-001', 10, 10, 10, 'Склад W-001', 'Основной склад', false)
        RETURNING id INTO w_id;
    END IF;

    DELETE FROM inventory_history WHERE warehouse_id = w_id;
    DELETE FROM product_warehouse  WHERE warehouse_id = w_id;
    DELETE FROM ai_predictions     WHERE warehouse_id = w_id;

    DELETE FROM product_warehouse WHERE product_id IN (
        SELECT id FROM products WHERE sku_code IN (
            'ELEC-001','ELEC-002','ELEC-003','ELEC-004',
            'FURN-001','FURN-002','OFFC-001','OFFC-002','OFFC-003'));
    DELETE FROM inventory_history WHERE product_id IN (
        SELECT id FROM products WHERE sku_code IN (
            'ELEC-001','ELEC-002','ELEC-003','ELEC-004',
            'FURN-001','FURN-002','OFFC-001','OFFC-002','OFFC-003'));
    DELETE FROM products WHERE sku_code IN (
        'ELEC-001','ELEC-002','ELEC-003','ELEC-004',
        'FURN-001','FURN-002','OFFC-001','OFFC-002','OFFC-003');

    CREATE TEMP TABLE IF NOT EXISTS _seed_wh (warehouse_id bigint) ON COMMIT DROP;
    INSERT INTO _seed_wh VALUES (w_id);
END $$;

-- 1. Статусы инвентаризации ──────────────────────────────────────────────────
INSERT INTO inventory_status (id, code, is_deleted)
SELECT 1, 'OK',        false WHERE NOT EXISTS (SELECT 1 FROM inventory_status WHERE id = 1);
INSERT INTO inventory_status (id, code, is_deleted)
SELECT 2, 'LOW_STOCK', false WHERE NOT EXISTS (SELECT 1 FROM inventory_status WHERE id = 2);
INSERT INTO inventory_status (id, code, is_deleted)
SELECT 3, 'CRITICAL',  false WHERE NOT EXISTS (SELECT 1 FROM inventory_status WHERE id = 3);

-- 2. Продукты ─────────────────────────────────────────────────────────────────
INSERT INTO products (sku_code, name, category, is_deleted, user_id)
SELECT v.sku_code, v.name, v.category, false, 1
FROM (VALUES
    ('ELEC-001','Смартфон Galaxy S25',   'Electronics'),
    ('ELEC-002','Ноутбук ThinkPad X1',    'Electronics'),
    ('ELEC-003','Беспроводные наушники',  'Electronics'),
    ('ELEC-004','Монитор 4K',             'Electronics'),
    ('FURN-001','Офисное кресло Ergo',    'Furniture'),
    ('FURN-002','Стол регулируемый',      'Furniture'),
    ('OFFC-001','Бумага A4 500л',         'Office'),
    ('OFFC-002','Картридж лазерный',      'Office'),
    ('OFFC-003','Степлер',                'Office')
) AS v(sku_code, name, category)
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.sku_code = v.sku_code AND p.is_deleted = false);

-- 3. product_warehouse ────────────────────────────────────────────────────────
INSERT INTO product_warehouse (product_id, warehouse_id, min_stock, optimal_stock, is_deleted)
SELECT p.id, w.warehouse_id, v.min_stock, v.optimal_stock, false
FROM (VALUES
    ('ELEC-001',10, 40),('ELEC-002', 5, 20),('ELEC-003',20,100),
    ('ELEC-004', 5, 12),('FURN-001',10, 25),('FURN-002', 5, 10),
    ('OFFC-001',100,400),('OFFC-002',10, 30),('OFFC-003',15, 60)
) AS v(sku_code, min_stock, optimal_stock)
JOIN products p ON p.sku_code = v.sku_code AND p.is_deleted = false
CROSS JOIN _seed_wh w
WHERE NOT EXISTS (
    SELECT 1 FROM product_warehouse pw
    WHERE pw.product_id = p.id AND pw.warehouse_id = w.warehouse_id
);

-- 4. Маппинг sku_code → product_id ───────────────────────────────────────────
CREATE TEMP TABLE _seed_pmap ON COMMIT DROP AS
SELECT sku_code, id AS product_id FROM products
WHERE sku_code IN ('ELEC-001','ELEC-002','ELEC-003','ELEC-004',
                   'FURN-001','FURN-002','OFFC-001','OFFC-002','OFFC-003')
  AND is_deleted = false;

-- 5. История инвентаризации (60 дней на продукт) через PL/pgSQL ──────────────
DO $$
DECLARE
    wh_id    bigint;
    p_id     bigint;
    sku      text;
    base_qty int;
    step_val int;
    start_dt date := '2026-04-15';
    d        date;
    cur_qty  int;
    row_num  int;
    sku_list text[] := ARRAY[
        'ELEC-001','ELEC-002','ELEC-003','ELEC-004',
        'FURN-001','FURN-002','OFFC-001','OFFC-002','OFFC-003'];
    -- daily_usage: ежедневное потребление (шт/день)
    usage    int[]  := ARRAY[1, 1, 3, 1, 1, 1, 13, 1, 2];
    start_qty int[] := ARRAY[80,40,200,24,50,20,800,60,120];
    zone_arr int[]  := ARRAY[1, 1, 1, 2, 2, 2, 3, 3, 3];
    row_arr  int[]  := ARRAY[1, 2, 3, 1, 2, 3, 1, 2, 3];
    shelf_arr int[] := ARRAY[1, 1, 1, 1, 1, 1, 1, 1, 1];
    cycle_len int := 14;
    prev_qty  int;
    exp_qty   int;
    diff_val  int;
    st_id     int;
BEGIN
    SELECT warehouse_id INTO wh_id FROM _seed_wh;

    FOR i IN 1..array_length(sku_list, 1) LOOP
        sku := sku_list[i];
        SELECT product_id INTO p_id FROM _seed_pmap m WHERE m.sku_code = sku;
        IF p_id IS NULL THEN CONTINUE; END IF;
        base_qty := start_qty[i];
        step_val := usage[i];
        prev_qty := base_qty;

        FOR row_num IN 0..59 LOOP
            d := start_dt + row_num;
            -- quantity: плавное убывание + пополнение каждые cycle_len дней
            cur_qty := base_qty - (row_num % cycle_len) * step_val - (random()*2)::int;
            IF cur_qty < 0 THEN cur_qty := 0; END IF;

            -- expected_quantity = то что было вчера (prev_qty), а не текущее
            -- тогда difference = cur_qty - prev_qty = потребление за день
            exp_qty := prev_qty;
            diff_val := cur_qty - exp_qty;  -- отрицательное = убыль

            -- status: по days_until_stockout = cur_qty / abs(usage)
            IF cur_qty <= 0 THEN st_id := 3;
            ELSIF cur_qty < step_val * 7 THEN st_id := 3;  -- CRITICAL: менее 7 дней
            ELSIF cur_qty < step_val * 30 THEN st_id := 2; -- MEDIUM: менее 30 дней
            ELSE st_id := 1; END IF;

            INSERT INTO inventory_history
                (warehouse_id, robot_id, product_id,
                 expected_quantity, quantity, difference, status_id,
                 scanned_at, is_deleted, zone, "row", shelf)
            VALUES (
                wh_id, NULL, p_id,
                exp_qty, cur_qty, diff_val, st_id,
                d + time '09:00:00', false,
                zone_arr[i], row_arr[i], shelf_arr[i]
            );

            prev_qty := cur_qty;
        END LOOP;
    END LOOP;
END $$;

-- 6. Синхронизация sequence ──────────────────────────────────────────────────
SELECT setval('inventory_history_id_seq', (SELECT MAX(id) FROM inventory_history));
SELECT setval('product_warehouse_id_seq',  (SELECT MAX(id) FROM product_warehouse));
SELECT setval('products_id_seq',           (SELECT MAX(id) FROM products));

COMMIT;

-- 7. Проверка ─────────────────────────────────────────────────────────────────
SELECT 'inventory_history' AS tbl, COUNT(*) AS rows FROM inventory_history;
