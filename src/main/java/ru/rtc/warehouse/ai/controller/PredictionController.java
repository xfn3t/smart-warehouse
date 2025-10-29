package ru.rtc.warehouse.ai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.rtc.warehouse.ai.service.PredictionService;

import java.util.Map;

@RestController
@RequestMapping("/api/predict")
@RequiredArgsConstructor
public class PredictionController {

	private final PredictionService predictionService;

	@GetMapping("/{productId}")
	public Map<String, Object> predict(
			@PathVariable Long productId,
			@RequestParam(defaultValue = "7") int horizon
	) {
		return predictionService.predictStock(productId, horizon);
	}
}
