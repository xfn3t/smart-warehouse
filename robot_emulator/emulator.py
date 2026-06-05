import os
import random
import threading
import time
import traceback
import zlib
from datetime import datetime, timezone
from typing import Dict, List, Optional

import jwt as pyjwt
import psycopg2
import psycopg2.extras
import requests

# Config (env or defaults)
API_URL = os.getenv("API_URL", "http://localhost:8080")
MODE = os.getenv("MODE", "live").lower()  # live or offline
ROBOTS_COUNT = int(os.getenv("ROBOTS_COUNT", "0"))
UPDATE_INTERVAL = float(os.getenv("UPDATE_INTERVAL", "8"))
STATUS_INTERVAL = float(os.getenv("STATUS_INTERVAL", str(max(5, UPDATE_INTERVAL))))
ROBOT_AUTH_TOKEN = os.getenv("ROBOT_AUTH_TOKEN", None)
JWT_SECRET = os.getenv("JWT_SECRET", "secret-key")

POSTGRES_HOST = os.getenv("POSTGRES_HOST", "localhost")
POSTGRES_PORT = int(os.getenv("POSTGRES_PORT", "5432"))
POSTGRES_DB = os.getenv("POSTGRES_DB", "smart_warehouse")
POSTGRES_USER = os.getenv("POSTGRES_USER", "warehouse_user")
POSTGRES_PASSWORD = os.getenv("POSTGRES_PASSWORD", "warehouse_pass")

SIM_P_MISSING = float(os.getenv("SIM_P_MISSING", "0.03"))
SIM_P_SWAPPED = float(os.getenv("SIM_P_SWAPPED", "0.01"))
SIM_P_COUNT_ERROR = float(os.getenv("SIM_P_COUNT_ERROR", "0.06"))

LOG_PREFIX = "[EMU-DB]"


# Util
def now_iso():
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%fZ")


def log(*args, **kwargs):
    print(LOG_PREFIX, *args, **kwargs)


def _loc_to_lock_key(loc_id) -> int:
    if loc_id is None:
        return 0
    try:
        if isinstance(loc_id, int):
            return int(loc_id)
        s = str(loc_id)
        if s.isdigit():
            return int(s)
    except Exception:
        pass
    return zlib.adler32(str(loc_id).encode("utf-8"))


# DB Adapter
class DbAdapter:
    def __init__(self, host, port, db, user, password):
        self.conn_params = {
            "host": host,
            "port": port,
            "dbname": db,
            "user": user,
            "password": password,
        }

    def _get_conn(self):
        params = dict(self.conn_params)
        for k, v in list(params.items()):
            if isinstance(v, str):
                clean = (
                    v.replace("\ufeff", "")
                    .replace("\u00a0", " ")
                    .replace("\u200b", "")
                    .replace("\ufffd", "")
                    .strip()
                )
                params[k] = clean
        params["client_encoding"] = "UTF8"
        if "options" not in params:
            params["options"] = "-c client_encoding=UTF8"
        import os
        os.environ["PGCLIENTENCODING"] = "UTF8"
        try:
            log("DB connect params (cleaned):")
            for k, v in params.items():
                if k == "password":
                    log(f"  {k} = [HIDDEN]")
                else:
                    log(f"  {k} = {repr(v)}")
            return psycopg2.connect(**params)
        except UnicodeDecodeError as e:
            log(f"UnicodeDecodeError details: {e}")
            log(f"Error at position: {e.start}-{e.end}")
            return self._get_conn_fallback(params)
        except Exception as e:
            log(f"Connection error: {e}")
            raise

    def _get_conn_fallback(self, params):
        try:
            dsn_parts = []
            for k, v in params.items():
                if k not in ["options", "client_encoding"] and v is not None:
                    dsn_parts.append(f"{k}={v}")
            dsn_parts.append("client_encoding=UTF8")
            dsn = " ".join(dsn_parts)
            log(f"Trying fallback DSN: {dsn.replace('password=warehouse_pass', 'password=[HIDDEN]')}")
            return psycopg2.connect(dsn)
        except Exception as e:
            log(f"Fallback connection also failed: {e}")
            raise

    def get_warehouses(self) -> List[Dict]:
        q = """
            SELECT id, code, zone_max_size, row_max_size, shelf_max_size, name
            FROM warehouses
            WHERE is_deleted = FALSE
        """
        try:
            with self._get_conn() as conn:
                with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                    cur.execute(q)
                    return cur.fetchall()
        except Exception as e:
            log("get_warehouses error:", e)
            return []

    def get_robots(self) -> List[Dict]:
        q = """
            SELECT r.id, r.robot_code, r.warehouse_id, w.code AS warehouse_code,
                   r.status_id, r.battery_level, r.location_id
            FROM robots r
            LEFT JOIN warehouses w ON w.id = r.warehouse_id
            WHERE r.is_deleted = FALSE
        """
        try:
            with self._get_conn() as conn:
                with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                    cur.execute(q)
                    rows = cur.fetchall()
                    for r in rows:
                        if "robot_code" not in r and "code" in r:
                            r["robot_code"] = r["code"]
                    return rows
        except Exception as e:
            log("get_robots error:", e)
            return []

    def get_locations_for_warehouse(self, warehouse_code_or_id) -> List[Dict]:
        try:
            with self._get_conn() as conn:
                with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                    if isinstance(warehouse_code_or_id, int) or str(warehouse_code_or_id).isdigit():
                        cur.execute(
                            """
                            SELECT id, zone, row, shelf
                            FROM location
                            WHERE warehouse_id = %s
                            """,
                            (int(warehouse_code_or_id),),
                        )
                    else:
                        cur.execute(
                            "SELECT id FROM warehouses WHERE code = %s LIMIT 1",
                            (warehouse_code_or_id,),
                        )
                        w = cur.fetchone()
                        if w:
                            cur.execute(
                                """
                                SELECT id, zone, row, shelf
                                FROM location
                                WHERE warehouse_id = %s
                                """,
                                (w["id"],),
                            )
                        else:
                            return []
                    return cur.fetchall()
        except Exception as e:
            log("get_locations_for_warehouse error:", e)
            return []

    def get_location_contents(self, location: Dict) -> List[Dict]:
        try:
            loc_id = location.get("id")
            if not loc_id:
                return []
            if not (isinstance(loc_id, int) or str(loc_id).isdigit()):
                return []
            q = """
                SELECT DISTINCT ON (ih.product_id) ih.product_id, p.sku_code, p.name, ih.quantity, ih.scanned_at
                FROM inventory_history ih
                JOIN products p ON p.id = ih.product_id
                WHERE ih.location_id = %s
                ORDER BY ih.product_id, ih.scanned_at DESC
            """
            with self._get_conn() as conn:
                with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                    cur.execute(q, (loc_id,))
                    rows = cur.fetchall()
                    products = []
                    for r in rows:
                        products.append({
                            "product_id": r.get("product_id"),
                            "sku_code": r.get("sku_code"),
                            "name": r.get("name"),
                            "quantity": r.get("quantity"),
                            "scanned_at": r.get("scanned_at"),
                        })
                    return products
        except Exception as e:
            log("get_location_contents error:", e)
            return []

    def _post(self, url: str, payload: Dict, token: Optional[str]) -> bool:
        headers = {"Content-Type": "application/json"}
        if token:
            headers["Authorization"] = f"Bearer {token}"
        try:
            resp = requests.post(url, json=payload, headers=headers, timeout=6)
            if resp.ok:
                return True
            else:
                log(f"POST {url} -> {resp.status_code} {resp.text}")
        except Exception as e:
            log(f"POST {url} error:", e)
        return False

    def post_robot_data(self, robot_code: str, payload: Dict, token: Optional[str]) -> bool:
        url = f"{API_URL.rstrip('/')}/api/robots/data"
        return self._post(url, payload, token)

    def post_robot_status(self, robot_code: str, payload: Dict, token: Optional[str]) -> bool:
        url = f"{API_URL.rstrip('/')}/api/robots/status"
        return self._post(url, payload, token)

    def get_robot_token(self, robot_code: str) -> Optional[str]:
        q = """
            SELECT rt.token
            FROM robot_tokens rt
            JOIN robots r ON r.id = rt.robot_id
            WHERE r.robot_code = %s
            ORDER BY rt.created_at DESC
            LIMIT 1
        """
        try:
            with self._get_conn() as conn:
                with conn.cursor() as cur:
                    cur.execute(q, (robot_code,))
                    row = cur.fetchone()
                    return row[0] if row else None
        except Exception as e:
            log("get_robot_token error:", e)
            return None

    def get_tokens_for_robot_codes(self, robot_codes: List[str]) -> Dict[str, str]:
        if not robot_codes:
            return {}
        codes = list(dict.fromkeys(robot_codes))
        q = """
            SELECT DISTINCT ON (r.robot_code) r.robot_code, rt.token
            FROM robot_tokens rt
            JOIN robots r ON r.id = rt.robot_id
            WHERE r.robot_code = ANY(%s)
            ORDER BY r.robot_code, rt.created_at DESC
        """
        try:
            with self._get_conn() as conn:
                with conn.cursor() as cur:
                    cur.execute(q, (codes,))
                    rows = cur.fetchall()
                    return {r[0]: r[1] for r in rows}
        except Exception as e:
            log("get_tokens_for_robot_codes error:", e)
            return {}

    def ensure_robot_exists(self, robot_code: str, warehouse_code: str) -> bool:
        try:
            with self._get_conn() as conn:
                with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                    cur.execute(
                        "SELECT id FROM robots WHERE robot_code = %s AND is_deleted = FALSE",
                        (robot_code,),
                    )
                    if cur.fetchone():
                        return True
                    cur.execute(
                        "SELECT id FROM warehouses WHERE code = %s AND is_deleted = FALSE LIMIT 1",
                        (warehouse_code,),
                    )
                    wh = cur.fetchone()
                    if not wh:
                        log(f"Cannot create robot {robot_code}: warehouse {warehouse_code} not found in DB")
                        return False
                    cur.execute(
                        "SELECT id FROM robot_status WHERE code = 'WORKING' LIMIT 1"
                    )
                    st = cur.fetchone()
                    if not st:
                        cur.execute(
                            "INSERT INTO robot_status (code, is_deleted) VALUES ('WORKING', FALSE) RETURNING id"
                        )
                        conn.commit()
                        st = cur.fetchone()
                    if not st:
                        log(f"Cannot create robot {robot_code}: no status")
                        return False
                with conn.cursor() as cur:
                    cur.execute(
                        """
                        INSERT INTO robots (robot_code, warehouse_id, status_id, battery_level, is_deleted)
                        VALUES (%s, %s, %s, 100, FALSE)
                        """,
                        (robot_code, wh["id"], st["id"]),
                    )
                    conn.commit()
            log(f"Created robot {robot_code} in warehouse {warehouse_code}")
            return True
        except Exception as e:
            log(f"ensure_robot_exists error for {robot_code}: {e}")
            return False

    def get_warehouse_products(self, warehouse_code: str) -> List[Dict]:
        try:
            q = """
                SELECT p.sku_code, p.name
                FROM products p
                JOIN product_warehouse pw ON pw.product_id = p.id
                JOIN warehouses w ON w.id = pw.warehouse_id
                WHERE w.code = %s
                  AND p.is_deleted = FALSE
                  AND pw.is_deleted = FALSE
                ORDER BY p.sku_code
            """
            with self._get_conn() as conn:
                with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                    cur.execute(q, (warehouse_code,))
                    return cur.fetchall()
        except Exception as e:
            log("get_warehouse_products error:", e)
            return []

    def ensure_robot_token(self, robot_code: str) -> Optional[str]:
        try:
            with self._get_conn() as conn:
                with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                    cur.execute(
                        "SELECT id, robot_code, warehouse_id FROM robots WHERE robot_code = %s AND is_deleted = FALSE",
                        (robot_code,),
                    )
                    robot = cur.fetchone()
                    if not robot:
                        log(f"Robot {robot_code} not found in DB, cannot create token")
                        return None
                    cur.execute(
                        """
                        SELECT rt.token FROM robot_tokens rt
                        WHERE rt.robot_id = %s AND rt.revoked = FALSE
                        ORDER BY rt.created_at DESC LIMIT 1
                        """,
                        (robot["id"],),
                    )
                    existing = cur.fetchone()
                    if existing:
                        log(f"Robot {robot_code} already has a valid token")
                        return existing["token"]
            now_dt = datetime.now(timezone.utc)
            claims = {
                "roles": ["ROBOT"],
                "warehouse_id": robot.get("warehouse_id", 1),
                "robot_code": robot_code,
            }
            token = pyjwt.encode(
                {"sub": robot_code, "iat": now_dt, **claims},
                JWT_SECRET,
                algorithm="HS256",
            )
            with self._get_conn() as conn:
                with conn.cursor() as cur:
                    cur.execute(
                        """
                        INSERT INTO robot_tokens (robot_id, token, created_at, revoked)
                        VALUES (%s, %s, %s, FALSE)
                        """,
                        (robot["id"], token, now_dt),
                    )
                    conn.commit()
            log(f"Created token for {robot_code}")
            return token
        except Exception as e:
            log(f"ensure_robot_token error for {robot_code}: {e}")
            return None

    def try_advisory_lock(self, conn, loc_id) -> bool:
        try:
            key = _loc_to_lock_key(loc_id)
            with conn.cursor() as cur:
                cur.execute("SELECT pg_try_advisory_lock(%s)", (key,))
                r = cur.fetchone()
                return bool(r and r[0])
        except Exception as e:
            log("try_advisory_lock error:", e)
            return False

    def advisory_unlock(self, conn, loc_id) -> bool:
        try:
            key = _loc_to_lock_key(loc_id)
            with conn.cursor() as cur:
                cur.execute("SELECT pg_advisory_unlock(%s)", (key,))
                r = cur.fetchone()
                return bool(r and r[0])
        except Exception as e:
            log("advisory_unlock error:", e)
            return False


# EmulatorManager class
class EmulatorManager:
    def __init__(self, api: DbAdapter, mode="live"):
        self.api = api
        self.mode = mode
        self.warehouses: Dict[str, Dict] = {}
        self.locations_by_wh: Dict[str, List[Dict]] = {}
        self.robots: List[Dict] = []
        self.routes_by_robot: Dict[str, List[Dict]] = {}

    def load(self):
        if self.mode == "live":
            whs = self.api.get_warehouses()
            if not whs:
                log("No warehouses in DB, switching to offline")
                self.mode = "offline"
            else:
                for w in whs:
                    code = w.get("code") or str(w.get("id"))
                    self.warehouses[code] = w
                for code in list(self.warehouses.keys()):
                    locs = self.api.get_locations_for_warehouse(code)
                    if not locs:
                        w = self.warehouses[code]
                        z = w.get("zone_max_size", 1)
                        r = w.get("row_max_size", 5)
                        s = w.get("shelf_max_size", 10)
                        locs = self._fabricate_locations(code, z, r, s)
                    self.locations_by_wh[code] = locs
                robots = self.api.get_robots()
                if not robots:
                    log("No robots found in DB; will create offline robots if requested")
                    self.robots = []
                else:
                    normalized = []
                    for r in robots:
                        rc = r.get("robot_code") or r.get("robotCode") or r.get("code")
                        wc = r.get("warehouse_code") or r.get("warehouseCode") or None
                        normalized.append({"robot_code": rc, "warehouse_code": wc})
                    self.robots = normalized
        if self.mode == "offline":
            code = "WH-EMU-1"
            wh = {
                "id": 9999,
                "code": code,
                "zone_max_size": 3,
                "row_max_size": 8,
                "shelf_max_size": 6,
                "name": "EMULATED",
            }
            self.warehouses[code] = wh
            locs = self._fabricate_locations(code, wh["zone_max_size"], wh["row_max_size"], wh["shelf_max_size"])
            skus = [f"SKU-{i:04d}" for i in range(1, 101)]
            for i, L in enumerate(locs):
                if random.random() < 0.5:
                    L["products"] = [{
                        "sku_code": random.choice(skus),
                        "name": f"Product {i}",
                        "quantity": random.randint(1, 50),
                    }]
            self.locations_by_wh[code] = locs
            count = ROBOTS_COUNT or 4
            self.robots = [{"robot_code": f"RB-EMU-{i+1:04d}", "warehouse_code": code} for i in range(count)]
            log(f"Offline: created {code} locations={len(locs)} robots={len(self.robots)}")

    def _fabricate_locations(self, code, zones, rows, shelves):
        arr = []
        idx = 1
        for z in range(1, zones+1):
            for r in range(1, rows+1):
                for s in range(1, shelves+1):
                    arr.append({"id": idx, "label": f"{code}-{idx}", "zone": z, "row": r, "shelf": s})
                    idx += 1
        return arr

    def plan_routes(self):
        robots_grouped = {}
        for r in self.robots:
            wc = r.get("warehouse_code")
            if not wc and len(self.warehouses) == 1:
                wc = next(iter(self.warehouses.keys()))
            if not wc or wc not in self.locations_by_wh:
                log(f"Robot {r.get('robot_code')} has no valid warehouse ({wc}), skipping")
                continue
            robots_grouped.setdefault(wc, []).append(r)
        for wc, robots in robots_grouped.items():
            locs = self.locations_by_wh.get(wc, [])
            if not locs:
                log(f"No locations for warehouse {wc}, skipping robots: {[r['robot_code'] for r in robots]}")
                continue
            n = len(robots) or 1
            for i, r in enumerate(robots):
                assigned = [loc for idx, loc in enumerate(locs) if (idx % n) == i]
                random.shuffle(assigned)
                self.routes_by_robot[r["robot_code"]] = assigned
                log(f"Assigned {len(assigned)} locations to {r['robot_code']} in {wc}")

    def get_route_for(self, robot_code):
        return self.routes_by_robot.get(robot_code, [])


# RobotWorker
class RobotWorker(threading.Thread):
    def __init__(self, robot_code, warehouse_code, route, api: DbAdapter, token, update_interval, status_interval):
        super().__init__(daemon=True)
        self.robot_code = robot_code
        self.warehouse_code = warehouse_code
        self.route = route[:]
        self.api = api
        self.token = token
        self.update_interval = update_interval
        self.status_interval = status_interval
        self.battery = 100.0
        self.running = True
        self.last_data_sent = None
        self._last_scan_fp: Dict[str, str] = {}
        self._warehouse_products: List[Dict] = []

    def run(self):
        if not self.route:
            log(f"{self.robot_code} empty route -> stopping")
            return
        idx = 0
        last_status_t = time.time() - self.status_interval
        while self.running:
            try:
                loc = self.route[idx % len(self.route)]
                self.scan_and_post(loc)
                self.last_data_sent = now_iso()
                now = time.time()
                if now - last_status_t >= self.status_interval:
                    self.post_status()
                    last_status_t = now
                idx += 1
                self.battery -= random.uniform(0.2, 0.8)
                if self.battery < 18:
                    log(f"{self.robot_code} low battery -> charging")
                    time.sleep(2 * self.update_interval)
                    self.battery = 100.0
                time.sleep(self.update_interval * random.uniform(0.85, 1.25))
            except Exception as e:
                log(f"Worker {self.robot_code} error: {e}\n{traceback.format_exc()}")
                time.sleep(max(1, self.update_interval))

    def stop(self):
        self.running = False

    def scan_and_post(self, location):
        try:
            if not self._warehouse_products:
                self._warehouse_products = self.api.get_warehouse_products(self.warehouse_code)
                if not self._warehouse_products:
                    whs = self.api.get_warehouses()
                    if whs:
                        self._warehouse_products = self.api.get_warehouse_products(whs[0].get("code", ""))
                if not self._warehouse_products:
                    log(f"{self.robot_code} no products found for warehouse {self.warehouse_code}, skipping scan")
                    return
                log(f"{self.robot_code} loaded {len(self._warehouse_products)} real products for {self.warehouse_code}")

            scan_results = []
            n_products = min(random.randint(1, 3), len(self._warehouse_products))
            picked = random.sample(self._warehouse_products, n_products)
            for p in picked:
                sku = p.get("sku_code", "")
                name = p.get("name", "unknown")
                base_q = random.randint(5, 200)
                r = random.random()
                if r < SIM_P_MISSING:
                    q = 0
                    status_code = "CRITICAL"
                elif r < SIM_P_MISSING + SIM_P_SWAPPED:
                    other = random.choice([x for x in self._warehouse_products if x.get("sku_code") != sku] or [p])
                    scan_results.append({
                        "productCode": other.get("sku_code", sku),
                        "productName": other.get("name", name) + " (swapped)",
                        "quantity": random.randint(1, 10),
                        "statusCode": "OK",
                    })
                    continue
                elif r < SIM_P_MISSING + SIM_P_SWAPPED + SIM_P_COUNT_ERROR:
                    delta = int(max(-base_q, base_q * random.uniform(-0.2, 0.2)))
                    q = max(0, base_q + delta)
                    status_code = "LOW_STOCK" if q < 10 else "OK"
                else:
                    q = base_q
                    status_code = "OK" if q > 20 else ("LOW_STOCK" if q > 5 else "CRITICAL")
                scan_results.append({
                    "productCode": sku,
                    "productName": name,
                    "quantity": int(q),
                    "statusCode": status_code,
                })
            if not scan_results:
                return
            next_checkpoint = location.get("label") or f"{location.get('zone',0)}-{location.get('row',0)}-{location.get('shelf',0)}"
            payload = {
                "code": self.robot_code,
                "timestamp": now_iso(),
                "location": {
                    "zone": int(location.get("zone", 0)),
                    "row": int(location.get("row", 0)),
                    "shelf": int(location.get("shelf", 0)),
                },
                "scanResults": scan_results,
                "batteryLevel": int(round(self.battery)),
                "nextCheckpoint": str(next_checkpoint),
            }
            token = self.token
            if not token:
                try:
                    token = self.api.get_robot_token(self.robot_code)
                    if token:
                        log(f"{self.robot_code} got token from DB")
                except Exception:
                    pass
            if not token:
                try:
                    self.api.ensure_robot_exists(self.robot_code, self.warehouse_code)
                    token = self.api.ensure_robot_token(self.robot_code)
                    if token:
                        self.token = token
                        log(f"{self.robot_code} created and cached token")
                except Exception:
                    pass
            ok = self.api.post_robot_data(self.robot_code, payload, token or ROBOT_AUTH_TOKEN)
            if ok:
                log(f"{self.robot_code} scanned {location.get('zone')}-{location.get('row')}-{location.get('shelf')} -> {len(scan_results)}")
            else:
                try:
                    log(f"{self.robot_code} failed to post scan for {location.get('id')}; payload={payload}")
                except Exception:
                    log(f"{self.robot_code} failed to post scan for {location.get('id')}")
        except Exception as e:
            log(f"{self.robot_code} scan_and_post error: {e}\n{traceback.format_exc()}")

    def post_status(self):
        from datetime import datetime
        current_time = datetime.now(timezone.utc)
        timestamp_iso = current_time.strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"
        payload = {
            "robotId": self.robot_code,
            "timestamp": timestamp_iso,
            "status": "WORKING" if self.battery > 20 else "CHARGING",
            "batteryLevel": int(round(self.battery)),
        }
        if self.last_data_sent:
            last_sent_dt = datetime.fromisoformat(self.last_data_sent.replace("Z", "+00:00"))
            last_sent_iso = last_sent_dt.strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"
            payload["lastDataSent"] = last_sent_iso
        token = self.token or ROBOT_AUTH_TOKEN
        if not token:
            try:
                self.api.ensure_robot_exists(self.robot_code, self.warehouse_code)
                token = self.api.ensure_robot_token(self.robot_code)
                if token:
                    self.token = token
            except Exception:
                pass
        ok = self.api.post_robot_status(self.robot_code, payload, token)
        if not ok:
            log(f"{self.robot_code} failed to post status")
            log(f"Status payload: {payload}")
            log(f"Using token: {'Yes' if token else 'No'}")


# Runner
def main():
    log("Starting emulator (DB mode)", "MODE=", MODE)
    db = DbAdapter(POSTGRES_HOST, POSTGRES_PORT, POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD)
    manager = EmulatorManager(db, mode=MODE)
    manager.load()
    if MODE == "offline" and ROBOTS_COUNT:
        pass
    elif MODE == "live":
        if not manager.robots:
            log("No robots found in DB and not in offline mode. Nothing to emulate.")
            return
    manager.plan_routes()
    robot_codes = [r.get("robot_code") for r in manager.robots if r.get("robot_code")]
    tokens_map = db.get_tokens_for_robot_codes(robot_codes)
    for rc in robot_codes:
        wc = next((r.get("warehouse_code") for r in manager.robots if r.get("robot_code") == rc), next(iter(manager.warehouses.keys()), "WH-EMU-1"))
        db.ensure_robot_exists(rc, wc)
        if rc not in tokens_map:
            new_token = db.ensure_robot_token(rc)
            if new_token:
                tokens_map[rc] = new_token
    workers = []
    for r in manager.robots:
        robot_code = r.get("robot_code")
        wc = r.get("warehouse_code") or next(iter(manager.warehouses.keys()))
        route = manager.get_route_for(robot_code)
        if not route:
            route = manager.locations_by_wh.get(wc, [])[:]
        token = tokens_map.get(robot_code) or ROBOT_AUTH_TOKEN
        worker = RobotWorker(
            robot_code=robot_code,
            warehouse_code=wc,
            route=route,
            api=db,
            token=token,
            update_interval=UPDATE_INTERVAL,
            status_interval=STATUS_INTERVAL,
        )
        worker.start()
        workers.append(worker)
        log(f"Started worker {robot_code} (route len={len(route)})")
    try:
        while True:
            time.sleep(60)
    except KeyboardInterrupt:
        log("Stopping workers...")
        for w in workers:
            w.stop()
        time.sleep(2)
        log("Exited.")


if __name__ == "__main__":
    main()