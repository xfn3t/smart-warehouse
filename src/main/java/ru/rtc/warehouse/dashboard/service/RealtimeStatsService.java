// src/main/java/ru/rtc/warehouse/dashboard/RealtimeStatsService.java
package ru.rtc.warehouse.dashboard.service;

import ru.rtc.warehouse.dashboard.dto.RealtimeStatsDTO;

/** Чтение real-time метрик для дашборда. */
public interface RealtimeStatsService {
    RealtimeStatsDTO getStats(String timezone);
}
