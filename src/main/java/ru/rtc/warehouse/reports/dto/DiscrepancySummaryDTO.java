package ru.rtc.warehouse.reports.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class DiscrepancySummaryDTO {
	private String warehouseCode;
	private String warehouseName;
	private String skuCode;
	private String productName;
	private Long scanCount;
	private Long totalAbsDiscrepancy;
	private BigDecimal avgDiscrepancy;
	private Long maxDiscrepancy;
	private LocalDateTime lastDiscrepancyAt;
}
