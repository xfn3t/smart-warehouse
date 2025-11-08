package ru.rtc.warehouse.ai.service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.rtc.warehouse.ai.service.dto.MlPredictionResponse;
import ru.rtc.warehouse.ai.service.dto.MlPredictionRequest;

import java.util.List;

@FeignClient(name = "predictionClient", url = "${ml.api.url}")
public interface PredictionClient {

	@PostMapping("/api/predict")
	MlPredictionResponse predict(@RequestBody List<MlPredictionRequest> features);
}