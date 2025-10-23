package ru.rtc.warehouse.ai.controller.dto;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.rtc.warehouse.ai.controller.dto.request.StockControllerRequest;
import ru.rtc.warehouse.ai.service.PredictionService;
import ru.rtc.warehouse.ai.service.feign.dto.response.PredictionResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PredictionController {

	private final PredictionService predictionService;

	@PostMapping("/predict")
	public PredictionResponse predict(@RequestBody List<StockControllerRequest> request) {
		return predictionService.getPrediction(request);
	}
}