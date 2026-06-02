package ru.rtc.warehouse.reports.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class WarehouseSummaryReportDTO {
	private Long warehouseId;
	private String warehouseCode;
	private String warehouseName;
	private Long totalQuantity;
	private Long uniqueSkuCount;
	private Long totalDiscrepancy;
	private Long totalAbsDiscrepancy;
	private LocalDateTime lastScanAt;
	private Long criticalCount;
	private Long lowStockCount;
	private LocalDateTime reportCreatedAt;
}
