package ru.rtc.warehouse.ai.service;

import ru.rtc.warehouse.ai.controller.dto.request.StockControllerRequest;
import ru.rtc.warehouse.ai.service.feign.dto.response.PredictionResponse;

import java.util.List;

public interface PredictionService {
	PredictionResponse getPrediction(List<StockControllerRequest> request);
}
