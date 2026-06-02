package ru.rtc.warehouse.reports.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DailyAggregationDTO {
	private Long warehouseId;
	private String warehouseCode;
	private LocalDate scanDate;
	private Long totalScans;
	private Long totalQuantity;
	private Long totalDifference;
	private Long totalAbsDifference;
	private Long uniqueProductsScanned;
	private Long uniqueRobots;
}
