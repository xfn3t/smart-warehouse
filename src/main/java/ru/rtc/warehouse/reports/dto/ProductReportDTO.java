package ru.rtc.warehouse.reports.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProductReportDTO {
	private Long productId;
	private String skuCode;
	private String productName;
	private String category;
	private String warehouseCode;
	private String warehouseName;
	private Integer minStock;
	private Integer optimalStock;
	private Integer currentQuantity;
	private Integer expectedQuantity;
	private Integer difference;
	private String inventoryStatus;
	private LocalDateTime lastScannedAt;
}
