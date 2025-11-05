package ru.rtc.warehouse.dashboard.dto.location;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
class LocationMetricsDTO {
	private String location_id;
	private Integer zone;
	private Integer row;
	private Integer shelf;
	private Integer total_products;
	private Integer capacity_percent;
	private LocalDateTime last_scan;
	private LocationStatsDTO metrics;
}