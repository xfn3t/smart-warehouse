package ru.rtc.warehouse.dashboard.dto.location;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class LocationMetricsDTO {

    private Integer zone;
    private Integer row;
    private Integer shelf;
    private Integer total_products;
    private Integer capacity_percent;
    private LocalDateTime last_scan;
    private LocationStatsDTO metrics;
}
