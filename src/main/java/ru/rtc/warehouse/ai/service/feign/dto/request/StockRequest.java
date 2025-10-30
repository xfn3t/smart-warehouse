package ru.rtc.warehouse.ai.service.feign.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class StockRequest {
	private String category;
	private int current_stock;
	private double avg_daily_sales;
	private int min_stock;
	private int optimal_stock;
	private double seasonal_factor;
}