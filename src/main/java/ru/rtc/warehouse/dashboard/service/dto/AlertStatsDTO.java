package ru.rtc.warehouse.dashboard.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AlertStatsDTO {
	private int lowStockAlerts;
	private int outOfStockAlerts;
}