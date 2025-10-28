package ru.rtc.warehouse.dashboard.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.dashboard.config.DashboardRealtimeProperties;
import ru.rtc.warehouse.inventory.common.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.robot.common.enums.RobotStatus;
import ru.rtc.warehouse.robot.model.Robot;

import java.time.*;

/**
 * Пишет atomic-метрики в Redis. Никакой логики БД — только счётчики/множества.
 */
@Component
@RequiredArgsConstructor
public class RealtimeMetricsWriter {

    private final StringRedisTemplate rt;
    private final DashboardRealtimeProperties props;

    /** Применить инкременты по записи истории. */
    public void onHistoryCreated(InventoryHistory ih) {
        if (ih == null) return;

        // 1) Проверено сегодня (по проектной таймзоне)
        ZoneId zone = ZoneId.of(props.getTimezone());
        LocalDate day = ih.getScannedAt()
                .atZone(zone).toLocalDate();
        String dayKey = RealtimeRedisKeys.checkedToday(day);
        rt.opsForValue().increment(dayKey);
        rt.expire(dayKey, Duration.ofDays(props.getTtl().getCheckedDayDays()));

        // 2) Поминутная активность (UTC)
        long epochMin = RealtimeRedisKeys.epochMinute(ih.getScannedAt().toInstant(ZoneOffset.UTC));
        String minuteKey = RealtimeRedisKeys.activityMinute(epochMin);
        rt.opsForValue().increment(minuteKey);
        rt.expire(minuteKey, Duration.ofSeconds(props.getTtl().getMinuteSeriesSeconds()));

        // 3) Критические SKU — по последнему статусу (приближённо)
        Product p = ih.getProduct();
        if (p != null && p.getCode() != null) {
            String sku = p.getCode();
            if (ih.getStatus() == InventoryHistoryStatus.CRITICAL) {
                rt.opsForSet().add(RealtimeRedisKeys.criticalSkuSet(), sku);
            } else {
                // если статус ок/низкий – удаляем из критических
                rt.opsForSet().remove(RealtimeRedisKeys.criticalSkuSet(), sku);
            }
        }
    }

    /** Применить обновление по роботу: множества активных/всех и средний заряд. */
    public void onRobotSnapshot(Robot r) {
        if (r == null || r.getCode() == null) return;
        String code = r.getCode();

        // Множества всех/активных
        rt.opsForSet().add(RealtimeRedisKeys.robotsAll(), code);
        if (r.getStatus() == RobotStatus.ACTIVE) {
            rt.opsForSet().add(RealtimeRedisKeys.robotsActive(), code);
        } else {
            rt.opsForSet().remove(RealtimeRedisKeys.robotsActive(), code);
        }

        // Средний заряд = сумма/кол-во, с учётом предыдущего значения
        if (r.getBatteryLevel() != null) {
            String hash = RealtimeRedisKeys.batteryHash();
            String prevStr = (String) rt.opsForHash().get(hash, code);
            long newVal = r.getBatteryLevel().longValue();

            if (prevStr == null) {
                // первого ещё нет: просто добавляем
                rt.opsForHash().put(hash, code, String.valueOf(newVal));
                rt.opsForValue().increment(RealtimeRedisKeys.batteryCnt());
                rt.opsForValue().increment(RealtimeRedisKeys.batterySum(), newVal);
            } else {
                long prev = Long.parseLong(prevStr);
                long diff = newVal - prev;
                if (diff != 0) {
                    rt.opsForHash().put(hash, code, String.valueOf(newVal));
                    rt.opsForValue().increment(RealtimeRedisKeys.batterySum(), diff);
                }
            }
        }
    }
}
