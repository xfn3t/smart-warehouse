import json
import time
import random
import requests
from datetime import datetime, timezone
import os
import threading

class RobotEmulator:
    def __init__(self, robot_id, api_url):
        self.robot_id = robot_id
        self.api_url = api_url.rstrip("/")
        self.battery = 100.0
        self.current_zone = 'A'
        self.current_row = 1
        self.current_shelf = 1

        # Список тестовых товаров (SKU как строка)
        self.products = [
            {"id": "TEL-4567", "name": "Роутер RT-AC68U"},
            {"id": "TEL-8901", "name": "Модем DSL-2640U"},
            {"id": "TEL-2345", "name": "Коммутатор SG-108"},
            {"id": "TEL-6789", "name": "IP-телефон T46S"},
            {"id": "TEL-3456", "name": "Кабель UTP Cat6"}
        ]

        # состояние сетевой симуляции
        self.consecutive_failures = 0
        self.last_data_sent_at = None

        # токен для авторизации (по умолчанию берём общий из env)
        self.auth_token = os.getenv('ROBOT_AUTH_TOKEN', None)

    def generate_scan_data(self):
        scanned_products = random.sample(self.products, k=random.randint(1, 3))
        scan_results = []
        for product in scanned_products:
            quantity = random.randint(5, 100)
            status = "OK" if quantity > 20 else ("LOW_STOCK" if quantity > 10 else "CRITICAL")
            # NOTE: productCode -> matches backend DTO
            scan_results.append({
                "productCode": product["id"],
                "productName": product["name"],
                "quantity": quantity,
                "status": status
            })
        return scan_results

    def move_to_next_location(self):
        self.current_shelf += 1
        if self.current_shelf > 10:
            self.current_shelf = 1
            self.current_row += 1
            if self.current_row > 20:
                self.current_row = 1
                # Переход к следующей зоне
                self.current_zone = chr(ord(self.current_zone) + 1)
                if ord(self.current_zone) > ord('E'):
                    self.current_zone = 'A'
        # Расход батареи
        self.battery -= random.uniform(0.1, 0.5)
        if self.battery < 20:
            self.battery = 100.0  # симуляция зарядки

    def send_data(self):
        data = {
            # field "code" соответствует RobotDataRequest.code
            "code": self.robot_id,
            "timestamp": datetime.utcnow().replace(tzinfo=timezone.utc).isoformat(),
            "location": {
                "zone": self.current_zone,
                "row": self.current_row,
                "shelf": self.current_shelf
            },
            "scanResults": self.generate_scan_data(),
            "batteryLevel": int(round(self.battery)),
            "nextCheckpoint": f"{self.current_zone}-{self.current_row+1}-{self.current_shelf}"
        }

        # Header: если есть глобальный токен в env, используем его; иначе fallback
        token = self.auth_token or f"robot_token_{self.robot_id}"
        headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }

        try:
            # Случайная имитация временных сетевых проблем (раз в N попыток)
            if random.random() < 0.05:  # 5% шанс симулировать локальный сетевой сбой
                raise requests.exceptions.ConnectionError("Simulated network failure")

            resp = requests.post(f"{self.api_url}/api/robots/data", json=data, headers=headers, timeout=5)
            if resp.status_code == 200:
                print(f"[{self.robot_id}] Data sent successfully")
                self.consecutive_failures = 0
                self.last_data_sent_at = datetime.utcnow().replace(tzinfo=timezone.utc)
                return True
            else:
                print(f"[{self.robot_id}] Data error: {resp.status_code} {resp.text}")
                self.consecutive_failures += 1
                return False
        except Exception as e:
            print(f"[{self.robot_id}] Data connection error: {e}")
            self.consecutive_failures += 1
            return False

    def send_status(self):
        # Определяем статус локально по количестве consecutive_failures
        if self.consecutive_failures == 0:
            status = "CONNECTED"
        elif self.consecutive_failures < 3:
            status = "RECONNECTING"
        else:
            status = "OFFLINE"

        payload = {
            # Note: backend controller expects robotId in status DTO; keep this key
            "robotId": self.robot_id,
            "timestamp": datetime.utcnow().replace(tzinfo=timezone.utc).isoformat(),
            "status": status,
            "batteryLevel": int(round(self.battery)),
            "lastDataSent": self.last_data_sent_at.isoformat() if self.last_data_sent_at else None
        }

        token = self.auth_token or f"robot_token_{self.robot_id}"
        headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }

        try:
            resp = requests.post(f"{self.api_url}/api/robots/status", json=payload, headers=headers, timeout=3)
            if resp.status_code == 200:
                # print(f"[{self.robot_id}] Status posted: {status}")
                return True
            else:
                print(f"[{self.robot_id}] Status error: {resp.status_code} {resp.text}")
                return False
        except Exception as e:
            print(f"[{self.robot_id}] Status connection error: {e}")
            return False

    def run(self):
        update_interval = int(os.getenv('UPDATE_INTERVAL', 10))
        status_interval = int(os.getenv('STATUS_INTERVAL', update_interval))  # по умолчанию совпадает
        last_status_time = 0

        while True:
            now_ts = time.time()
            sent = self.send_data()
            # обновляем позицию только после попытки отправки (как раньше)
            self.move_to_next_location()

            # Отправлять статус не реже, чем каждые status_interval секунд
            if now_ts - last_status_time >= status_interval:
                self.send_status()
                last_status_time = now_ts

            time.sleep(update_interval)


if __name__ == "__main__":
    api_url = os.getenv('API_URL', 'http://localhost:8080')
    robots_count = int(os.getenv('ROBOTS_COUNT', 5))
    threads = []
    for i in range(1, robots_count + 1):
        robot_id = f"RB-{i:03d}"
        emulator = RobotEmulator(robot_id, api_url)
        t = threading.Thread(target=emulator.run, daemon=True)
        t.start()
        threads.append(t)

    # Держим главный поток активным
    try:
        while True:
            time.sleep(60)
    except KeyboardInterrupt:
        print("Stopping emulators...")
