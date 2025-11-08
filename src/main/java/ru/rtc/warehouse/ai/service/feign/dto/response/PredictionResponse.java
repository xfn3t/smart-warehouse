package ru.rtc.warehouse.ai.service.feign.dto.response;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@Getter
@Setter
public class PredictionResponse {
	private List<Prediction> predictions;

	@Getter
	@Setter
	public static class Prediction {
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

		@JsonProperty("predicted_stock_7d")
		private double predictedStock7d;

		@JsonProperty("days_until_stockout")
		private int daysUntilStockout;

		@JsonProperty("recommended_order")
		private double recommendedOrder;
	}
}