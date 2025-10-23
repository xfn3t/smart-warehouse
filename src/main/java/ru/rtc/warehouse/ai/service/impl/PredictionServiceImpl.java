package ru.rtc.warehouse.ai.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.ai.controller.dto.request.StockControllerRequest;
import ru.rtc.warehouse.ai.service.InventoryHistoryAdapter;
import ru.rtc.warehouse.ai.service.PredictionService;
import ru.rtc.warehouse.ai.service.feign.PredictionClient;
import ru.rtc.warehouse.ai.service.feign.dto.request.StockRequest;
import ru.rtc.warehouse.ai.service.feign.dto.response.PredictionResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PredictionServiceImpl implements PredictionService {

	private final PredictionClient predictionClient;
	private final InventoryHistoryAdapter ihAdapter;

	public PredictionResponse getPrediction(List<StockControllerRequest> controllerRequests) {

		List<StockRequest> requests = controllerRequests.stream()
				.map(controllerRequest -> StockRequest.builder()
						.category(controllerRequest.getCategory())
						.current_stock(controllerRequest.getCurrentStock())
						.avg_daily_sales(ihAdapter.avgDailySales().doubleValue())
						.min_stock(controllerRequest.getMinStock())
						.optimal_stock(controllerRequest.getOptimalStock())
						.seasonal_factor(ihAdapter.seasonalFactor().doubleValue())
						.build()
				)
				.toList();


		return predictionClient.predict(requests);
	}
}