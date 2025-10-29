package ru.rtc.warehouse.ai.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.ai.service.InventoryDataAggregationService;
import ru.rtc.warehouse.ai.service.PredictionService;
import ru.rtc.warehouse.ai.service.feign.PredictionClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PredictionServiceImpl implements PredictionService {

	private final InventoryDataAggregationService aggregationService;
	private final PredictionClient predictionClient;

	public Map<String, Object> predictStock(Long productId, int horizonDays) {
		Map<String, Object> featureSet = aggregationService.buildFeatureSet(productId, horizonDays);
		return predictionClient.predict(featureSet);
	}
}
