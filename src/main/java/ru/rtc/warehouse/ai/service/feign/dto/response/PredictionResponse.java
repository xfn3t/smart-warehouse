package ru.rtc.warehouse.ai.service.feign.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PredictionResponse {

	private List<Prediction> predictions;

	@Getter
	@Setter
	public static class Prediction {
		private String category;
		private int current_stock;
		private double avg_daily_sales;
		private int min_stock;
		private int optimal_stock;
		private double seasonal_factor;
		private double predicted_stock_7d;
		private int days_until_stockout;
		private double recommended_order;
	}
}