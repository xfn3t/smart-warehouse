package ru.rtc.warehouse.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class WarehouseStatsDTO {
	private String warehouse_code;
	private LocalDateTime timestamp;
	private WarehouseMetricsDTO metrics;
}