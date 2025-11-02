package ru.rtc.warehouse.dashboard.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.dashboard.config.DashboardRealtimeProperties;
import ru.rtc.warehouse.dashboard.dto.RealtimeStatsDTO;
import ru.rtc.warehouse.dashboard.redis.RealtimeRedisKeys;
import ru.rtc.warehouse.dashboard.service.RealtimeStatsService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    public RealtimeStatsDTO getStats(String warehouseCode) {
        ZoneId zone = RealtimeRedisKeys.zone();

        LocalDateTime toMinute = LocalDateTime.now(zone).truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime fromHour = toMinute.minusMinutes(59);

        long active = size(RealtimeRedisKeys.robotsActive(warehouseCode));
        long total = size(RealtimeRedisKeys.robotsAll(warehouseCode));
        long criticalSkus = size(RealtimeRedisKeys.criticalSkuSet(warehouseCode));

        long sum = parseLong(rt.opsForValue().get(RealtimeRedisKeys.batterySum(warehouseCode)));
        long cnt = parseLong(rt.opsForValue().get(RealtimeRedisKeys.batteryCnt(warehouseCode)));
        Double avgBattery = (cnt == 0L) ? null : (double) sum / cnt;

        long checkedToday = parseLong(rt.opsForValue().get(
                RealtimeRedisKeys.checkedToday(warehouseCode, LocalDate.now(zone))));

        List<RealtimeStatsDTO.ActivityPoint> series = new ArrayList<>(60);
        LocalDateTime cur = fromHour;
        while (!cur.isAfter(toMinute)) {
            long epochMin = RealtimeRedisKeys.epochMinute(cur);
            String key = RealtimeRedisKeys.activityMinute(warehouseCode, epochMin);
            long v = parseLong(rt.opsForValue().get(key));
            series.add(RealtimeStatsDTO.ActivityPoint.builder()
                    .ts(cur)
                    .count(v)
                    .build());
            cur = cur.plusMinutes(1);
        }

        return RealtimeStatsDTO.builder()
                .activeRobots(active)
                .totalRobots(total)
                .checkedToday(checkedToday)
                .criticalSkus(criticalSkus)
                .avgBatteryPercent(avgBattery)
                .activitySeries(series)
                .serverTime(LocalDateTime.now(zone))
                .build();
    }


    private long size(String key) { Long s = rt.opsForSet().size(key); return s == null ? 0L : s; }
    private long parseLong(String s) { if (s == null || s.isBlank()) return 0L; try { return Long.parseLong(s); } catch (Exception e) { return 0L; } }
}
