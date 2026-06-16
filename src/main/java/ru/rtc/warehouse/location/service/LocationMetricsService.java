package ru.rtc.warehouse.location.service;

import ru.rtc.warehouse.location.dto.LocationMetricsDTO;
import ru.rtc.warehouse.warehouse.model.Warehouse;

public interface LocationMetricsService {
    LocationMetricsDTO computeFor(
        Warehouse warehouse,
        Integer zone,
        Integer row,
        Integer shelf
    );
}
