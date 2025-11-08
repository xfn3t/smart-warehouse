package ru.rtc.warehouse.ai.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisPredictionDTO {
	private String sku;
	private Integer daysUntilStockout;
	private Integer recommendedOrder;
	private String criticalLevel;
	private BigDecimal confidenceScore;
	private String warehouseCode;
	private Long lastUpdated;
	private Double quantity;
	private Double expectedQuantity;
	private Double difference;
	private Double minStock;
	private Double optimalStock;
}