package ru.rtc.warehouse.location.service;

import ru.rtc.warehouse.location.dto.LocationMetricsDTO;
import ru.rtc.warehouse.location.model.Location;

public interface LocationMetricsService {
      public LocationMetricsDTO computeFor(Location loc);
}
