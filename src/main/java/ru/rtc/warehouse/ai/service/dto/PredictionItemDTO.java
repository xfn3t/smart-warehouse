package ru.rtc.warehouse.ai.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionItemDTO {
	private Double daysUntilStockout;
	private Double recommendedOrder;
	private String criticalLevel;
	private String sku;
}