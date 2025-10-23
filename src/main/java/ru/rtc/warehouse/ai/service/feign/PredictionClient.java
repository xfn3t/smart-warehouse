package ru.rtc.warehouse.ai.service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.rtc.warehouse.ai.service.feign.dto.request.StockRequest;
import ru.rtc.warehouse.ai.service.feign.dto.response.PredictionResponse;

import java.util.List;

@FeignClient(name = "predictionClient", url = "http://ai-service:8001")
public interface PredictionClient {

	@PostMapping("/api/ai/predict")
	PredictionResponse predict(@RequestBody List<StockRequest> request);
}