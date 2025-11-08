package ru.rtc.warehouse.ai.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MlPredictionRequest {
	private String sku;

	@JsonProperty("quantity")
	private Double quantity;

	@JsonProperty("expected_quantity")
	private Double expectedQuantity;

	@JsonProperty("difference")
	private Double difference;

	@JsonProperty("min_stock")
	private Double minStock;

	@JsonProperty("optimal_stock")
	private Double optimalStock;
}