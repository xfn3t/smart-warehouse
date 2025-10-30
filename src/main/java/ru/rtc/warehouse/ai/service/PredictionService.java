package ru.rtc.warehouse.ai.service;

import java.util.Map;

public interface PredictionService {
	Map<String, Object> predictStock(String sku, String warehouseCode);
}
