<<<<<<< HEAD
## Warehouse Management System

Комплексная система управления складскими запасами с AI-прогнозированием, системой аутентификации, реальным временем мониторинга, управлением инвентаризацией, товарами, местоположениями, роботами и пользователями.

## Описание

Система предоставляет REST API для управления всеми аспектами складских операций: прогнозирования запасов, аутентификации пользователей, мониторинга в реальном времени, полного цикла инвентаризации, управления товарами, складскими местоположениями, роботами-инвентаризаторами и пользователями с ролевой моделью доступа.

## Запуск
=======
# Start Prod
>>>>>>> 4dbb19b888aa4519bcd72bc9409171c2e0230009

```shell
docker-compose --env-file .env.dev up --build -d
```
<<<<<<< HEAD

## Основные возможности

### Модуль AI Prediction
- Прогнозирование сроков исчерпания запасов для товаров
- Рекомендации по объему заказа для пополнения запасов
- Интеграция с внешней ML-системой через Feign Client
- Хранение истории прогнозов в базе данных
- Поддержка мульти-складской архитектуры

### Модуль Authentication & Authorization
- Регистрация и аутентификация пользователей через email/пароль
- JWT-токены с системой access/refresh токенов
- Ролевая модель с поддержкой различных уровней доступа
- Аутентификация для роботов с перманентными токенами
- CORS настройки для крос-доменных запросов
- Spring Security с stateless сессиями

### Модуль Dashboard & Real-time Monitoring
- **Real-time статистика** по складам с WebSocket
- **Мониторинг активности** роботов и сканирований
- **Метрики в реальном времени**: активные роботы, критические SKU, уровень батареи
- **Автоматическое обновление** данных каждые 30 секунд
- **Графики активности** за последний час с поминутной детализацией
- **Redis-кэширование** метрик с TTL

### Модуль Inventory Management
- **Полный цикл инвентаризации**: создание, обновление, поиск и удаление записей
- **Массовая загрузка данных** через CSV-файлы с автоматической обработкой
- **Расширенный поиск и фильтрация** по датам, зонам, статусам, категориям, роботам
- **Сводная статистика** по инвентаризациям с аналитикой
- **Автоматическое определение статусов** (OK, LOW_STOCK, CRITICAL) на основе запасов
- **Генерация SKU** на основе местоположения товаров
- **Поиск товаров с низким запасом** для своевременного пополнения
- **Пагинация и сортировка** для работы с большими объемами данных

### Модуль Location Management
- **Управление складскими местоположениями** с координатами (зона, ряд, полка)
- **Автоматическая генерация локаций** для новых складов
- **Статусы локаций** (OLD, MEDIUM, RECENT) для управления приоритетом сканирования
- **Поиск локаций по координатам** и складу
- **Интеграция с инвентаризацией** для привязки товаров к конкретным местам

### Модуль Product Management
- **Управление товарными позициями** (SKU) на складе
- **Настройка параметров запасов**: минимальный и оптимальный запас
- **Категоризация товаров** для организации и фильтрации
- **Мягкое удаление** товаров с сохранением исторических данных
- **Поиск товаров** по коду, названию, категории
- **Интеграция с системой инвентаризации** для отслеживания остатков

### Модуль Robot Management
- **Управление роботами-инвентаризаторами** на складе
- **Автоматическая генерация кодов** роботов (RB-0001, RB-0002, ...)
- **Отслеживание статусов** роботов (IDLE, WORKING, CHARGING, ERROR)
- **Мониторинг уровня заряда батареи** и текущего местоположения
- **Интеграция с аутентификацией** - генерация JWT токенов для роботов
- **Статистика производительности** - количество сканирований, эффективность работы
- **Автоматическое обновление** данных о местоположении и статусе

### Модуль User Management
- **Управление пользователями** системы
- **Ролевая модель доступа** (VIEWER, ADMIN, MANAGER, OPERATOR, ROBOT)
- **Привязка пользователей к складам** через связь many-to-many
- **Регистрация и управление профилями** пользователей
- **Интеграция с Spring Security** для аутентификации и авторизации

### Модуль Warehouse Management
- **Управление складскими помещениями** и их параметрами
- **Настройка размеров склада** (зоны, ряды, полки)
- **Автоматическая генерация локаций** при создании склада
- **Привязка пользователей** к конкретным складам
- **Обновление размеров** с перегенерацией локаций

## Структура проекта и модули

### Модуль AI Prediction
**Назначение:** Обработка запросов на прогнозирование и управление данными прогнозов

**Ключевые компоненты:**
- `PredictionController` - REST API для получения прогнозов
- `PredictionService` - сервис прогнозирования с интеграцией ML-системы
- `AiPrediction` - сущность для хранения прогнозов
- `PredictionClient` - Feign Client для ML-интеграции

### Модуль Authentication & Authorization
**Назначение:** Управление аутентификацией, авторизацией и пользовательскими сессиями

**Ключевые компоненты:**
- `SecurityConfig` - конфигурация Spring Security
- `AuthController` - эндпоинты для регистрации, входа, обновления токенов
- `AuthService` - сервис аутентификации с поддержкой refresh-токенов
- `JwtUtil` - утилиты для работы с JWT-токенами

### Модуль Dashboard & Real-time
**Назначение:** Мониторинг активности склада в реальном времени

**Ключевые компоненты:**
- `WebSocketConfig` - конфигурация WebSocket
- `RealtimeStatsController` - API для статистики в реальном времени
- `RealtimeMetricsWriter` - запись метрик в Redis
- `RealtimePushScheduler` - автоматическая отправка данных через WebSocket

### Модуль Inventory Management
**Назначение:** Полное управление процессами инвентаризации и историей складских операций

**Ключевые компоненты:**
- `InventoryHistory` - основная сущность истории инвентаризации
- `InventoryController` - загрузка CSV и основные операции
- `InventoryHistoryQueryController` - расширенный поиск и фильтрация
- `InventoryHistorySummaryController` - сводная статистика

### Модуль Location Management
**Назначение:** Управление складскими местоположениями и их статусами

**Ключевые компоненты:**
- `Location` - сущность местоположения с координатами
- `LocationService` - сервис для генерации и управления локациями
- `LocationStatus` - статусы локаций (OLD, MEDIUM, RECENT)

### Модуль Product Management
**Назначение:** Управление товарными позициями и их параметрами

**Ключевые компоненты:**
- `Product` - сущность товара с параметрами запасов
- `ProductService` - бизнес-логика управления товарами
- `ProductMapper` - преобразование между сущностями и DTO

### Модуль Robot Management
**Назначение:** Управление роботами-инвентаризаторами и их работой

**Ключевые компоненты:**

#### Основные сущности
- `Robot` - сущность робота с привязкой к складу и локации
- `RobotStatus` - статусы роботов (IDLE, WORKING, CHARGING, ERROR)

#### Контроллеры
- `RobotController` - API для управления роботами

#### Сервисы
- `RobotService` - бизнес-логика управления роботами
- `RobotEntityService` - CRUD операции и расширенная статистика
- `RobotStatusService` - управление статусами роботов

#### Особенности
- **Автогенерация кодов** роботов по шаблону RB-XXXX
- **JWT токены для роботов** через `RobotAuthService`
- **Интеграция с локациями** для отслеживания позиции
- **Статистика производительности** и эффективности

### Модуль User Management
**Назначение:** Управление пользователями и ролевой моделью доступа

**Ключевые компоненты:**

#### Основные сущности
- `User` - сущность пользователя с привязкой к ролям и складам
- `Role` - ролевая модель (VIEWER, ADMIN, MANAGER, OPERATOR, ROBOT)

#### Сервисы
- `UserService` - бизнес-логика управления пользователями
- `UserEntityService` - CRUD операции для пользователей
- `RoleService` - управление ролями и правами доступа

#### Интеграции
- **Связь со Spring Security** через `UserDetailsServiceImpl`
- **Привязка к складам** через отношение many-to-many
- **Интеграция с аутентификацией** для регистрации и входа

### Модуль Warehouse Management
**Назначение:** Управление складскими помещениями и их структурой

**Ключевые компоненты:**
- `Warehouse` - сущность склада с параметрами размеров
- `WarehouseController` - API для управления складами
- `WarehouseService` - бизнес-логика управления складами
- **Автоматическая генерация локаций** при изменении размеров

## Установка и зависимости

**Технологический стек:**
- Java + Spring Boot
- Spring Security + JWT
- Spring Data JPA (Hibernate)
- Spring Cloud OpenFeign
- Spring WebSocket + STOMP
- Redis для real-time метрик
- PostgreSQL для основного хранилища
- Liquibase для миграций БД
- MapStruct для маппинга объектов
- Lombok для генерации кода
- OpenCSV для обработки CSV-файлов

**Основные зависимости:**
- `spring-boot-starter-web`
- `spring-boot-starter-security`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-websocket`
- `spring-boot-starter-data-redis`
- `spring-cloud-starter-openfeign`
- `liquibase-core`
- `jjwt` для JWT-токенов
- `mapstruct`
- `lombok`
- `opencsv` для обработки CSV

## Конфигурация

В `application.yml`:
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: true
  redis:
    host: ${SPRING_REDIS_HOST:localhost}
    port: ${SPRING_REDIS_PORT:6379}
  liquibase:
    change-log: classpath:db/changelog/master-changelog-v1.1.yaml

server:
  port: ${APP_PORT:8080}

ml:
  api:
    url: http://ai-service:8001

security:
  jwt:
    secret: ${JWT_SECRET}
    access-token-exp-seconds: 86400000
    refresh-token-exp-seconds: 1209600

app:
  dashboard:
    ttl:
      minute-series-seconds: 4000
      checked-day-days: 3
```

## Использование

### Аутентификация

**Регистрация пользователя:**
```bash
POST /api/auth/register
{
  "email": "user@example.com",
  "password": "password123",
  "name": "John Doe",
  "role": "ADMIN"
}
```

**Вход в систему:**
```bash
POST /api/auth/login
{
  "email": "user@example.com", 
  "password": "password123"
}
```

### Управление роботами

**Создание робота:**
```bash
POST /api/robots
Authorization: Bearer {accessToken}
{
  "code": "RB-0001",
  "status": "IDLE",
  "batteryLevel": 85,
  "currentZone": 1,
  "currentRow": 5,
  "currentShelf": 3,
  "warehouseId": 1
}
```

**Получение всех роботов склада:**
```bash
GET /api/{warehouseCode}/robot
Authorization: Bearer {accessToken}
```

**Обновление робота:**
```bash
PUT /api/robots/{id}
Authorization: Bearer {accessToken}
{
  "status": "WORKING",
  "batteryLevel": 75,
  "currentZone": 2
}
```

### Управление пользователями

**Создание пользователя:**
```bash
POST /api/users
Authorization: Bearer {accessToken}
{
  "email": "operator@example.com",
  "password": "operator123",
  "name": "Operator User",
  "role": "OPERATOR"
}
```

### Управление складами

**Создание склада:**
```bash
POST /api/warehouse
Authorization: Bearer {accessToken}
{
  "code": "WH-001",
  "name": "Main Warehouse",
  "zoneMaxSize": 10,
  "rowMaxSize": 50,
  "shelfMaxSize": 20,
  "location": "Moscow, Industrial Zone"
}
```

**Получение складов пользователя:**
```bash
GET /api/warehouse
Authorization: Bearer {accessToken}
```

### Управление инвентаризацией

**Загрузка CSV-файла с инвентаризацией:**
```bash
POST /api/{warehouseCode}/inventory/upload-csv
Content-Type: multipart/form-data
Authorization: Bearer {accessToken}

file: <csv-file>
```

### Real-time мониторинг

**REST API для статистики:**
```bash
GET /api/{warehouseCode}/realtime/stats
Authorization: Bearer {accessToken}
```

**WebSocket подключение:**
```javascript
const stompClient = new Stomp.Client();
stompClient.webSocketFactory = () => new WebSocket('ws://localhost:8080/ws');
stompClient.onConnect = () => {
    stompClient.subscribe('/topic/realtime/{warehouseCode}', (message) => {
        const stats = JSON.parse(message.body);
        // Обновление UI
    });
};
stompClient.activate();
```

### Получение прогноза по запасам

```bash
GET /api/{warehouseCode}/predict?sku=SKU001&sku=SKU002
Authorization: Bearer {accessToken}
```

## База данных

**Основные таблицы:**
- `ai_predictions` - история AI-прогнозов
- `refresh_tokens` - refresh-токены пользователей
- `robot_tokens` - токены для роботов
- `inventory_history` - история инвентаризаций
- `inventory_status` - статусы инвентаризаций
- `robots` - данные роботов
- `robot_status` - статусы роботов
- `warehouses` - данные складов
- `products` - данные товаров
- `locations` - данные местоположений
- `location_status` - статусы локаций
- `users` - данные пользователей
- `roles` - роли пользователей
- `user_warehouses` - связь пользователей со складами

**Redis структуры:**
- `rt:{warehouse}:checked:{date}` - счетчики проверок за день
- `rt:{warehouse}:act:{epochMinute}` - активность по минутам
- `rt:{warehouse}:robots:active` - активные роботы
- `rt:{warehouse}:sku:critical` - критические SKU
- `rt:{warehouse}:robots:battery` - уровни батареи

## Особенности системы

### Модуль Robot
- **Автоматическая генерация кодов** по шаблону RB-XXXX с уникальной нумерацией
- **Полная интеграция с аутентификацией** - каждый робот получает JWT токен
- **Расширенная статистика** - эффективность, количество сканирований, уровень заряда
- **Real-time отслеживание** статусов и местоположения
- **Интеграция с системой событий** для обновления метрик в реальном времени

### Модуль User
- **Гибкая ролевая модель** с 5 уровнями доступа
- **Привязка пользователей к складам** - каждый пользователь может работать с несколькими складами
- **Интеграция с Spring Security** для полноценной аутентификации и авторизации
- **Поддержка мягкого удаления** с сохранением исторических данных

### Модуль Warehouse
- **Автоматическая генерация локаций** при создании или изменении размеров склада
- **Динамическое обновление структуры** при изменении параметров склада
- **Связь с пользователями** через отношение many-to-many
- **Полная интеграция** со всеми другими модулями системы

### Безопасность
- **JWT токены** для пользователей и роботов
- **Ролевая модель доступа** с различными уровнями привилегий
- **Привязка к конкретным складам** для ограничения доступа
- **Stateless аутентификация** с поддержкой refresh токенов
=======
# Start TestDB
```shell
docker compose -f docker-compose.test.yaml up -d
```
>>>>>>> 4dbb19b888aa4519bcd72bc9409171c2e0230009
