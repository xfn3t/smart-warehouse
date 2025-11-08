package ru.rtc.warehouse.ai.service.feign.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@Builder
public class StockRequest {
	private String category;

	@JsonProperty("current_stock")
	private int currentStock;

	@JsonProperty("avg_daily_sales")
	private double avgDailySales;

	@JsonProperty("min_stock")
	private int minStock;

	@JsonProperty("optimal_stock")
	private int optimalStock;

	@JsonProperty("seasonal_factor")
	private double seasonalFactor;
}