package ru.rtc.warehouse.dashboard.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.dashboard.config.DashboardRealtimeProperties;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus.InventoryHistoryStatusCode;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.model.RobotStatus.StatusCode;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Записывает атомарные метрики в Redis по событиям домена.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeMetricsWriter {

    private final StringRedisTemplate rt;
    private final DashboardRealtimeProperties props;

    /**
     * Обновляет метрики по созданной записи истории инвентаризации.
     */
    public void onHistoryCreated(InventoryHistory ih) {
        if (ih == null || ih.getScannedAt() == null) return;

        // 1) "Проверено сегодня" — по локальному дню САМОГО события
        LocalDateTime scannedAt = ih.getScannedAt();
        LocalDate day = scannedAt.toLocalDate();
        String dayKey = RealtimeRedisKeys.checkedToday(day);
        rt.opsForValue().increment(dayKey);
        ensureExpireIfAbsent(dayKey, Duration.ofDays(props.getTtl().getCheckedDayDays()));

        // 2) Поминутная активность — бакеты по локальному времени сервера
        long epochMinute = RealtimeRedisKeys.epochMinute(scannedAt);
        String minuteKey = RealtimeRedisKeys.activityMinute(epochMinute);
        rt.opsForValue().increment(minuteKey);
        ensureExpireIfAbsent(minuteKey, Duration.ofSeconds(props.getTtl().getMinuteSeriesSeconds()));

        // 3) Критические SKU — по коду товара и статусу записи
        Product p = ih.getProduct();
        if (p != null && p.getCode() != null && ih.getStatus() != null && ih.getStatus().getCode() != null) {
            String sku = p.getCode();
            if (ih.getStatus().getCode() == InventoryHistoryStatusCode.CRITICAL) {
                rt.opsForSet().add(RealtimeRedisKeys.criticalSkuSet(), sku);
            } else {
                // не критичный — исключаем из множества
                rt.opsForSet().remove(RealtimeRedisKeys.criticalSkuSet(), sku);
            }
        }
    }

    /**
     * Обновляет метрики по состоянию робота.
     */
    public void onRobotSnapshot(@Nullable Robot robot) {
        if (robot == null || robot.getCode() == null) return;

        final String code = robot.getCode();

        // Множества "все" / "активные"
        rt.opsForSet().add(RealtimeRedisKeys.robotsAll(), code);
        if (isActive(robot)) {
            rt.opsForSet().add(RealtimeRedisKeys.robotsActive(), code);
        } else {
            rt.opsForSet().remove(RealtimeRedisKeys.robotsActive(), code);
        }

        // Средний заряд: поддерживаем сумму/кол-во + последнее значение по роботу
        if (robot.getBatteryLevel() != null) {
            String hashKey = RealtimeRedisKeys.batteryHash();
            String prevStr = (String) rt.opsForHash().get(hashKey, code);
            long newVal = robot.getBatteryLevel().longValue();

            if (prevStr == null) {
                // первое наблюдение по этому роботу
                rt.opsForHash().put(hashKey, code, String.valueOf(newVal));
                rt.opsForValue().increment(RealtimeRedisKeys.batteryCnt());
                rt.opsForValue().increment(RealtimeRedisKeys.batterySum(), newVal);
            } else {
                long prev = parseLongSafe(prevStr, 0L);
                long diff = newVal - prev;
                if (diff != 0) {
                    rt.opsForHash().put(hashKey, code, String.valueOf(newVal));
                    rt.opsForValue().increment(RealtimeRedisKeys.batterySum(), diff);
                }
            }
        }
    }

    // -------------------- Вспомогательные методы --------------------

    /**
     * Устанавливает TTL для ключа, если у ключа отсутствует TTL.
     */
    private void ensureExpireIfAbsent(String key, Duration ttl) {
        try {
            Long currentTtl = rt.getExpire(key);
            if (currentTtl == null || currentTtl < 0) { // -1: нет TTL, -2: ключа нет
                rt.expire(key, ttl);
            }
        } catch (Exception e) {
            log.debug("Unable to set expire for key '{}': {}", key, e.getMessage());
        }
    }

    /**
     * Безопасный парсинг long.
     */
    private long parseLongSafe(String s, long def) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    /**
     * Признак «активного» робота для realtime-метрик.
     */
    private boolean isActive(Robot r) {
        return r.getStatus() != null && r.getStatus().getCode() == StatusCode.WORKING;
    }
}