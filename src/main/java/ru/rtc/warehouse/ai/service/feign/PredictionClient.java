package ru.rtc.warehouse.ai.service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "predictionClient", url = "${ml.api.url}")
public interface PredictionClient {

	@PostMapping("/api/predict")
	Map<String, Object> predict(@RequestBody List<Map<String, Object>> features);
}