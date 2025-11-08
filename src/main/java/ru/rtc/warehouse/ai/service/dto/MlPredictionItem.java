package ru.rtc.warehouse.ai.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class MlPredictionItem {
	@JsonProperty("days_until_stockout")
	private Double daysUntilStockout;

	@JsonProperty("recommended_order")
	private Double recommendedOrder;

	@JsonProperty("critical_level")
	private String criticalLevel;

	private String sku;
}