package ru.rtc.warehouse.ai.service;

import java.util.List;
import java.util.Map;

public interface PredictionService {
	Map<String, Object> predictStock(List<String> sku, String warehouseCode);
}
