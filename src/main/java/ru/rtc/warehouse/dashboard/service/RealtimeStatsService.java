package ru.rtc.warehouse.dashboard.service;

import ru.rtc.warehouse.dashboard.dto.RealtimeStatsDTO;

/** Чтение real-time метрик для дашборда. */
public interface RealtimeStatsService {
    RealtimeStatsDTO getStats(String warehouseCode);
}
