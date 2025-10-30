package ru.rtc.warehouse.ai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.rtc.warehouse.ai.service.PredictionService;

import java.util.Map;

@RestController
@RequestMapping("/api/{warehouseCode}/predict")
@RequiredArgsConstructor
public class PredictionController {

	private final PredictionService predictionService;

	@GetMapping("/{productId}")
	public Map<String, Object> predict(
			@PathVariable String warehouseCode,
			@RequestParam String sku
	) {
		return predictionService.predictStock(sku, warehouseCode);
	}
}
