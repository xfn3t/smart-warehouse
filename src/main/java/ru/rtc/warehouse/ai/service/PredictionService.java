package ru.rtc.warehouse.ai.service;

import ru.rtc.warehouse.ai.service.feign.dto.response.StockPredictionResponse;

import java.util.List;

public interface PredictionService {
	StockPredictionResponse predictStock(List<String> sku, String warehouseCode);
}