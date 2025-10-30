package ru.rtc.warehouse.ai.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.ai.service.InventoryHistoryEntAdapter;
import ru.rtc.warehouse.ai.service.PredictionService;
import ru.rtc.warehouse.ai.service.feign.PredictionClient;
import ru.rtc.warehouse.inventory.model.InventoryHistory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PredictionServiceImpl implements PredictionService {

	private final PredictionClient predictionClient;
	private final InventoryHistoryEntAdapter ihea;

	private Map<String, Object> buildFeatureSet(String sku, String warehouseCode) {

		InventoryHistory inventoryHistory = ihea.findByProductSKU(sku, warehouseCode);

		Map<String, Object> featureSet = new HashMap<>();
		featureSet.put("sku", sku);
		featureSet.put("quantity", inventoryHistory.getQuantity());
		featureSet.put("expected_quantity", inventoryHistory.getExpectedQuantity());
		featureSet.put("difference", inventoryHistory.getDifference());
		featureSet.put("min_stock", inventoryHistory.getProduct().getMinStock());
		featureSet.put("optimal_stock", inventoryHistory.getProduct().getOptimalStock());

		return featureSet;
	}

	public Map<String, Object> predictStock(List<String> skus, String warehouseCode) {

		List<Map<String, Object>> request = new ArrayList<>();
		for (String sku : skus) {
			request.add(buildFeatureSet(sku, warehouseCode));
		}
		return predictionClient.predict(request);
	}
}
