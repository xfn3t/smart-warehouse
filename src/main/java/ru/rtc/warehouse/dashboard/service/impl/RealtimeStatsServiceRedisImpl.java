package ru.rtc.warehouse.dashboard.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.dashboard.service.RealtimeStatsService;
import ru.rtc.warehouse.dashboard.config.DashboardRealtimeProperties;
import ru.rtc.warehouse.dashboard.dto.RealtimeStatsDTO;
import ru.rtc.warehouse.dashboard.redis.RealtimeRedisKeys;

import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Реализация чтения real-time метрик из Redis.
 */
@Service
@RequiredArgsConstructor
public class RealtimeStatsServiceRedisImpl implements RealtimeStatsService {

    private final StringRedisTemplate rt;
    private final DashboardRealtimeProperties props;

    @Override
    @Transactional(readOnly = true)
    public RealtimeStatsDTO getStats(String timezone) {
        ZoneId zone = (timezone == null || timezone.isBlank())
                ? ZoneId.of(props.getTimezone()) : ZoneId.of(timezone);

        Instant now = Instant.now();
        Instant toMinute = now.truncatedTo(ChronoUnit.MINUTES);
        Instant fromHour = toMinute.minus(59, ChronoUnit.MINUTES); // 60 точек включая текущую

        // Активные/всего роботов
        long active = rt.opsForSet().size(RealtimeRedisKeys.robotsActive()) == null ? 0L
                : rt.opsForSet().size(RealtimeRedisKeys.robotsActive());
        long total = rt.opsForSet().size(RealtimeRedisKeys.robotsAll()) == null ? 0L
                : rt.opsForSet().size(RealtimeRedisKeys.robotsAll());

        // Проверено сегодня (по указанной таймзоне в контроллере/конфиге)
        LocalDate today = LocalDate.now(zone);
        String dayKey = RealtimeRedisKeys.checkedToday(today);
        long checkedToday = parseLong(rt.opsForValue().get(dayKey));

        // Критические SKU по последним сканам
        long criticalSkus = rt.opsForSet().size(RealtimeRedisKeys.criticalSkuSet()) == null ? 0L
                : rt.opsForSet().size(RealtimeRedisKeys.criticalSkuSet());

        // Средний заряд батарей
        long sum = parseLong(rt.opsForValue().get(RealtimeRedisKeys.batterySum()));
        long cnt = parseLong(rt.opsForValue().get(RealtimeRedisKeys.batteryCnt()));
        Double avgBattery = (cnt == 0L) ? null : (double) sum / cnt;

        // Серия активности за последний час (60 точек)
        List<RealtimeStatsDTO.ActivityPoint> series = new ArrayList<>(60);
        Instant cur = fromHour;
        while (!cur.isAfter(toMinute)) {
            long epochMin = RealtimeRedisKeys.epochMinute(cur);
            String key = RealtimeRedisKeys.activityMinute(epochMin);
            long v = parseLong(rt.opsForValue().get(key));
            series.add(RealtimeStatsDTO.ActivityPoint.builder()
                    .ts(cur).count(v).build());
            cur = cur.plus(1, ChronoUnit.MINUTES);
        }

        return RealtimeStatsDTO.builder()
                .activeRobots(active)
                .totalRobots(total)
                .checkedToday(checkedToday)
                .criticalSkus(criticalSkus)
                .avgBatteryPercent(avgBattery)
                .activitySeries(series)
                .serverTimeUtc(now)
                .build();
    }

    private long parseLong(String s) {
        if (s == null || s.isBlank()) return 0L;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return 0L; }
    }
}
