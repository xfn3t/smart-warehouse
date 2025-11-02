#!/usr/bin/env python3
import os
import asyncio
import asyncpg
import random
import time
from datetime import datetime, timezone
import sys
import io
import aiohttp
import math


# ---------- Fix encoding ----------
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

# ---------- Config ----------
POSTGRES_HOST = os.getenv("POSTGRES_HOST", "localhost")
POSTGRES_PORT = int(os.getenv("POSTGRES_PORT", "5432"))
POSTGRES_DB = os.getenv("POSTGRES_DB", "smart_warehouse")
POSTGRES_USER = os.getenv("POSTGRES_USER", "warehouse_user")
POSTGRES_PASSWORD = os.getenv("POSTGRES_PASSWORD", "warehouse_pass")

API_URL = os.getenv("API_URL", "http://localhost:8080")

ADMIN_ACCESS_TOKEN = os.getenv("ADMIN_ACCESS_TOKEN", "")

WAREHOUSES = int(os.getenv("WAREHOUSES", "1"))
LOCS_PER_WH = int(os.getenv("LOCS_PER_WH", "120"))
PRODUCTS = int(os.getenv("PRODUCTS", "100"))
ROBOTS = int(os.getenv("ROBOTS", "4"))
CREATE_INVENTORY_HISTORY = os.getenv("CREATE_INVENTORY_HISTORY", "true").lower() in ("1","true","yes")
CLEAN = os.getenv("CLEAN", "false").lower() in ("1","true","yes")

LOG_PREFIX = "[SEED]"

# global robot sequence to ensure RB-001 ... RB-NNN unique across warehouses
ROBOT_SEQ = 1

def log(*args, **kwargs):
    print(LOG_PREFIX, *args, **kwargs)

async def get_conn():
    """Подключение к PostgreSQL через asyncpg"""
    try:
        conn = await asyncpg.connect(
            host=POSTGRES_HOST,
            port=POSTGRES_PORT,
            database=POSTGRES_DB,
            user=POSTGRES_USER,
            password=POSTGRES_PASSWORD
        )
        log("✅ Подключение к базе данных установлено")
        return conn
    except Exception as e:
        log(f"❌ Ошибка подключения: {e}")
        raise

async def ensure_extensions(conn):
    await conn.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto;")

async def clean_test_data(conn):
    """
    Удаляет только записи, которые соответствуют формату, создаваемому этим скриптом.
    ВНИМАНИЕ: запускать CLEAN=true только в тестовой базе!
    """
    log("🧹 Очистка тестовых данных (выполняется ТОЛЬКО по флагу CLEAN=true)...")
    # robot tokens starting with TOKEN-
    await conn.execute("DELETE FROM robot_tokens WHERE token LIKE 'TOKEN-%'")
    # inventory_history rows created by this script (best-effort)
    await conn.execute("DELETE FROM inventory_history WHERE message_id::text LIKE '0000%' OR created_at > now() - interval '7 days'")
    # robots whose code exactly matches RB-### pattern (three digits)
    await conn.execute("DELETE FROM robots WHERE robot_code ~ '^RB-[0-9]{3}$'")
    # products whose sku matches SKU-##### pattern
    await conn.execute("DELETE FROM products WHERE sku_code ~ '^SKU-[0-9]{5}$'")
    # locations belonging to warehouses with code WH-###
    await conn.execute("""
        DELETE FROM location
        WHERE warehouse_id IN (SELECT id FROM warehouses WHERE code ~ '^WH-[0-9]{3}$')
    """)
    # warehouses with code WH-###
    await conn.execute("DELETE FROM warehouses WHERE code ~ '^WH-[0-9]{3}$'")

async def insert_reference_rows(conn):
    await conn.execute("INSERT INTO roles (code) VALUES ('ROBOT') ON CONFLICT (code) DO NOTHING")
    await conn.execute("INSERT INTO robot_status (code) VALUES ('WORKING') ON CONFLICT (code) DO NOTHING")
    await conn.execute("INSERT INTO location_status (code) VALUES ('RECENT') ON CONFLICT (code) DO NOTHING")
    await conn.execute("INSERT INTO inventory_status (code) VALUES ('OK') ON CONFLICT (code) DO NOTHING")

async def create_warehouses(conn):
    wh_ids = []
    for i in range(1, WAREHOUSES + 1):
        code = f"WH-{i:03d}"
        name = f"Warehouse {i}"
        zone_max = 3
        row_max = 10
        shelf_max = max(6, LOCS_PER_WH // (zone_max * row_max) + 1)

        wid = await conn.fetchval("""
            INSERT INTO warehouses (code, zone_max_size, row_max_size, shelf_max_size, name)
            VALUES ($1,$2,$3,$4,$5)
            ON CONFLICT (code) DO UPDATE SET 
                zone_max_size=EXCLUDED.zone_max_size, 
                row_max_size=EXCLUDED.row_max_size, 
                shelf_max_size=EXCLUDED.shelf_max_size, 
                name=EXCLUDED.name
            RETURNING id
        """, code, zone_max, row_max, shelf_max, name)

        wh_ids.append((wid, code, zone_max, row_max, shelf_max))
        log(f"✅ Warehouse ensured: {code} (id={wid})")
    return wh_ids


async def create_locations(conn, wh_tuple):
    """
    Создаёт ровно LOCS_PER_WH уникальных локаций для заданного склада, но
    НЕ использует уникальные индексы в БД. Для безопасности при параллельных
    запусках используется pg_advisory_lock(warehouse_id).
    Если уже есть >= LOCS_PER_WH локаций — возвращаем существующие (первые).
    """
    wid, wcode, zones, rows, _shelves = wh_tuple

    # сколько полок нужно в теории
    shelves_needed = math.ceil(LOCS_PER_WH / (zones * rows))
    shelves = max(1, shelves_needed)

    locs = []

    # захватываем advisory lock для этого склада, чтобы избежать гонок
    # (замет: блокировка держится на соединении до pg_advisory_unlock или до закрытия соединения)
    lock_key = wid if isinstance(wid, int) else int(wid)
    try:
        await conn.execute("SELECT pg_advisory_lock($1)", lock_key)

        # получим все существующие координаты для склада в виде словаря (zone,row,shelf)->id
        rows_existing = await conn.fetch("""
            SELECT id, zone, row, shelf
            FROM location
            WHERE warehouse_id = $1
        """, wid)

        existing_map = {(r["zone"], r["row"], r["shelf"]): r["id"] for r in rows_existing}
        existing_count = len(existing_map)

        # Если уже достаточно — вернём первые LOCS_PER_WH записей (по id)
        if existing_count >= LOCS_PER_WH:
            fetched = await conn.fetch("""
                SELECT id, zone, row, shelf
                FROM location
                WHERE warehouse_id = $1
                ORDER BY id
                LIMIT $2
            """, wid, LOCS_PER_WH)
            locs = [{"id": r["id"], "zone": r["zone"], "row": r["row"], "shelf": r["shelf"]} for r in fetched]
            log(f"ℹ️ Already have {existing_count} locations for {wcode} (>= {LOCS_PER_WH}), returning first {len(locs)}")
            return locs

        # Начнём с уже существующих
        for (z, r, s), lid in existing_map.items():
            locs.append({"id": lid, "zone": z, "row": r, "shelf": s})

        created = existing_count
        # Генерируем координаты и вставляем те, которых нет
        for z in range(1, zones + 1):
            if created >= LOCS_PER_WH:
                break
            for r in range(1, rows + 1):
                if created >= LOCS_PER_WH:
                    break
                for s in range(1, shelves + 1):
                    if created >= LOCS_PER_WH:
                        break

                    coord = (z, r, s)
                    if coord in existing_map:
                        continue  # уже есть — пропускаем

                    # вставляем новую запись и получаем id
                    lid = await conn.fetchval("""
                        INSERT INTO location (zone, row, shelf, warehouse_id)
                        VALUES ($1,$2,$3,$4)
                        ON CONFLICT (warehouse_id, zone, row, shelf) DO NOTHING
                        RETURNING id;
                    """, z, r, s, wid)

                    # на всякий случай, если вставка не вернула id (маловероятно), получим через SELECT
                    if lid is None:
                        lid = await conn.fetchval("""
                            SELECT id FROM location
                            WHERE warehouse_id=$1 AND zone=$2 AND row=$3 AND shelf=$4
                        """, wid, z, r, s)

                    locs.append({"id": lid, "zone": z, "row": r, "shelf": s})
                    existing_map[coord] = lid
                    created += 1

        log(f"✅ Ensured {len(locs)} locations for {wcode} (id={wid}) using zones={zones}, rows={rows}, shelves={shelves}")
        return locs

    finally:
        # обязательно освобождаем блокировку
        try:
            await conn.execute("SELECT pg_advisory_unlock($1)", lock_key)
        except Exception:
            # если unlock упал — ничего критичного (соединение закроется в конце), но логируем
            log(f"⚠️ Failed to release advisory lock for warehouse id={wid}")


async def create_products(conn, wh_id):
    products = []
    for i in range(1, PRODUCTS + 1):
        sku = f"SKU-{i:05d}"
        name = f"Product {i}"

        pid = await conn.fetchval("""
            INSERT INTO products (warehouse_id, sku_code, name)
            VALUES ($1,$2,$3)
            ON CONFLICT (sku_code) DO UPDATE SET name = EXCLUDED.name
            RETURNING id
        """, wh_id, sku, name)

        products.append((pid, sku, name))

    log(f"✅ Ensured {len(products)} products for warehouse id={wh_id}")
    return products

# в секции Config (вверху файла) добавь/проверь:
ADMIN_ACCESS_TOKEN = os.getenv("ADMIN_ACCESS_TOKEN", "")


async def create_robots(conn, wh_id):
    """
    Create robots via API using RobotCreateRequest shape.
    If API returns access token (AuthResponse.accessToken) — save to robot_tokens and write to robot_tokens.txt.
    Falls back to DB-only insertion if API fails or returns 403.
    """
    global ROBOT_SEQ
    robots = []
    api_register = f"{API_URL.rstrip('/')}/api/robots/register"
    token_file = os.path.join(os.getcwd(), "robot_tokens.txt")

    headers_base = {"Content-Type": "application/json"}
    if ADMIN_ACCESS_TOKEN:
        headers_base["Authorization"] = f"Bearer {ADMIN_ACCESS_TOKEN}"
    else:
        log("⚠️ ADMIN_ACCESS_TOKEN not set — requests unauthenticated and may return 403.")

    # prefer status string 'WORKING' (matches your validation)
    status_code = "WORKING"

    async with aiohttp.ClientSession() as session:
        for _ in range(1, ROBOTS + 1):
            robot_code = f"RB-{ROBOT_SEQ:04d}"
            rid = None
            token = None

            # pick a random existing location for this warehouse (if any)
            loc_row = await conn.fetchrow("""
                SELECT zone, row, shelf FROM location
                WHERE warehouse_id = $1
                ORDER BY random()
                LIMIT 1
            """, wh_id)

            if loc_row:
                currentZone = int(loc_row["zone"])
                currentRow = int(loc_row["row"])
                currentShelf = int(loc_row["shelf"])
            else:
                # fallback sensible defaults
                currentZone = 1
                currentRow = 1
                currentShelf = 1

            payload = {
                "code": robot_code,
                "status": status_code,
                "batteryLevel": 100,
                "currentZone": currentZone,
                "currentRow": currentRow,
                "currentShelf": currentShelf,
                "warehouseId": wh_id
            }

            try:
                # try API register with a small retry loop
                for attempt in range(3):
                    try:
                        async with session.post(api_register, json=payload, headers=headers_base, timeout=10) as resp:
                            text = await resp.text()
                            if resp.status == 200:
                                try:
                                    j = await resp.json()
                                except Exception:
                                    j = {}
                                # controller returns AuthResponse: accessToken
                                token = None
                                if isinstance(j, dict):
                                    token = j.get("accessToken") or j.get("token") or j.get("access_token")
                                log(f"API register {robot_code} -> status {resp.status}")
                                break
                            elif resp.status == 403:
                                log(f"API register {robot_code} -> 403 Forbidden. Check ADMIN_ACCESS_TOKEN and permissions.")
                                break
                            else:
                                log(f"API register {robot_code} -> status {resp.status}, body: {text}")
                                await asyncio.sleep(0.5)
                    except aiohttp.ClientError as e:
                        log(f"API request error for {robot_code}: {e}")
                        await asyncio.sleep(0.5)

                # If token obtained, wait for robot row to appear in DB (service may have created it)
                if token:
                    for _retry in range(8):
                        rid = await conn.fetchval("SELECT id FROM robots WHERE robot_code = $1", robot_code)
                        if rid:
                            break
                        await asyncio.sleep(0.3)
                    if not rid:
                        rid = await conn.fetchval("""
                            INSERT INTO robots (robot_code, warehouse_id, battery_level, status_id, location_id)
                            VALUES ($1,$2,$3,$4,$5)
                            ON CONFLICT (robot_code) DO UPDATE SET battery_level = EXCLUDED.battery_level
                            RETURNING id
                        """, robot_code, wh_id, 100, None, None)

                    # write token to robot_tokens table
                    try:
                        await conn.execute("""
                            INSERT INTO robot_tokens (robot_id, token)
                            VALUES ($1,$2)
                            ON CONFLICT (token) DO NOTHING
                        """, rid, token)
                    except Exception as e:
                        log(f"Failed to insert token for {robot_code}: {e}")

                    # Append token to robot_tokens.txt
                    try:
                        with open(token_file, "a", encoding="utf-8") as fh:
                            fh.write(f"{robot_code} {token}\n")
                    except Exception as e:
                        log(f"Failed to write token file for {robot_code}: {e}")

                else:
                    # API did not provide token (or returned 403) -> create robot in DB (no token)
                    rid = await conn.fetchval("""
                        INSERT INTO robots (robot_code, warehouse_id, battery_level, status_id, location_id)
                        VALUES ($1,$2,$3,$4,$5)
                        ON CONFLICT (robot_code) DO UPDATE SET battery_level = EXCLUDED.battery_level
                        RETURNING id
                    """, robot_code, wh_id, 100, None, None)
                    log(f"Fallback: created robot {robot_code} in DB (no token)")

            except Exception as e:
                log(f"Unexpected error handling robot {robot_code}: {e}")
                rid = await conn.fetchval("""
                    INSERT INTO robots (robot_code, warehouse_id, battery_level, status_id, location_id)
                    VALUES ($1,$2,$3,$4,$5)
                    ON CONFLICT (robot_code) DO UPDATE SET battery_level = EXCLUDED.battery_level
                    RETURNING id
                """, robot_code, wh_id, 100, None, None)

            robots.append({"id": rid, "robot_code": robot_code})
            log(f"✅ Ensured robot {robot_code} (id={rid}) token={'YES' if token else 'NO'}")
            ROBOT_SEQ += 1

    return robots



async def create_inventory_history(conn, wh_id, locs, products, robots):
    if not CREATE_INVENTORY_HISTORY:
        return 0
    rows = []
    # use naive UTC datetime to avoid asyncpg timezone error
    now = datetime.now(timezone.utc).replace(tzinfo=None)
    # For each location add 1-3 product rows
    for L in locs:
        count_items = random.randint(1, min(3, len(products)))
        picked = random.sample(products, count_items)
        for p in picked:
            prod_id = p[0]
            qty = random.randint(0, 200)
            robot_id = random.choice(robots)["id"] if robots else None
            # message_id = None  # DB default gen_random_uuid will apply if omitted
            scanned_at = now
            diff = 0
            status_id = None
            rows.append((wh_id, robot_id, prod_id, L["id"], None, qty, diff, status_id, scanned_at))
    if rows:
        await conn.executemany("""
            INSERT INTO inventory_history
                (warehouse_id, robot_id, product_id, location_id, expected_quantity, quantity, difference, status_id, scanned_at)
            VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9)
        """, rows)
    log(f"✅ Inserted {len(rows)} inventory_history rows for WH id={wh_id}")
    return len(rows)


async def main():
    log("🚀 Seeder starting with config:", 
        f"WAREHOUSES={WAREHOUSES}", 
        f"LOCS_PER_WH={LOCS_PER_WH}",
        f"PRODUCTS={PRODUCTS}", 
        f"ROBOTS={ROBOTS}", 
        f"CLEAN={CLEAN}")

    conn = await get_conn()
    try:
        await ensure_extensions(conn)

        if CLEAN:
            await clean_test_data(conn)

        await insert_reference_rows(conn)
        wh_entries = await create_warehouses(conn)

        total_inv = 0
        for wh in wh_entries:
            wid = wh[0]
            locs = await create_locations(conn, wh)
            prods = await create_products(conn, wid)
            robs = await create_robots(conn, wid)
            total_inv += await create_inventory_history(conn, wid, locs, prods, robs)
            log(f"✅ Done for warehouse {wh[1]}")

        log("✅ Seeder finished successfully.")
        log(f"Created warehouses: {len(wh_entries)}, inventory rows: {total_inv}")

    except Exception as e:
        log("❌ Seeder failed:", e)
        raise
    finally:
        await conn.close()

if __name__ == "__main__":
    asyncio.run(main())
