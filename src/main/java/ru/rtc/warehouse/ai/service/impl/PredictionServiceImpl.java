package ru.rtc.warehouse.ai.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.ai.controller.dto.websocket.PredictionWebSocketDTO;
import ru.rtc.warehouse.ai.model.AiPrediction;
import ru.rtc.warehouse.ai.repository.AiPredictionRepository;
import ru.rtc.warehouse.ai.service.InventoryHistoryEntAdapter;
import ru.rtc.warehouse.ai.service.PredictionRedisService;
import ru.rtc.warehouse.ai.service.PredictionService;
import ru.rtc.warehouse.ai.service.dto.MlPredictionItem;
import ru.rtc.warehouse.ai.service.dto.MlPredictionRequest;
import ru.rtc.warehouse.ai.service.dto.PredictionItemDTO;
import ru.rtc.warehouse.ai.service.dto.RedisPredictionDTO;
import ru.rtc.warehouse.ai.service.feign.PredictionClient;
import ru.rtc.warehouse.ai.service.feign.dto.response.StockPredictionResponse;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.product.service.ProductEntityService;
import ru.rtc.warehouse.warehouse.model.Warehouse;
import ru.rtc.warehouse.warehouse.service.WarehouseEntityService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionServiceImpl implements PredictionService {

	private final PredictionClient predictionClient;
	private final InventoryHistoryEntAdapter inventoryHistoryEntAdapter;
	private final ProductEntityService productEntityService;
	private final WarehouseEntityService warehouseEntityService;
	private final AiPredictionRepository aiPredictionRepository;
	private final PredictionRedisService predictionRedisService;
	private final SimpMessagingTemplate messagingTemplate;

	private static final BigDecimal CONFIDENCE_SCORE = BigDecimal.valueOf(0.95);
	private static final String WS_PREDICTIONS_TOPIC = "/topic/dashboard/predictions";

	@Override
	@Transactional
	public StockPredictionResponse predictStock(List<String> skus, String warehouseCode) {
		log.info("=== STARTING PREDICTION PROCESS ===");
		log.info("Warehouse code: {}, SKUs: {}", warehouseCode, skus);

		Warehouse warehouse = warehouseEntityService.findByCode(warehouseCode);
		log.info("Found warehouse: {} (ID: {})", warehouse.getCode(), warehouse.getId());

		List<String> targetSkus = skus != null && !skus.isEmpty()
				? skus
				: getAllProductsForWarehouse(warehouseCode);

		log.info("Target SKUs to process: {}", targetSkus.size());
		if (targetSkus.isEmpty()) {
			log.warn("No products found for warehouse: {}", warehouseCode);
			throw new NotFoundException("No products found for warehouse: " + warehouseCode);
		}

		List<MlPredictionRequest> featureSets = buildFeatureSets(targetSkus, warehouseCode);
		log.info("Successfully built {} feature sets out of {} target SKUs",
				featureSets.size(), targetSkus.size());

		if (featureSets.isEmpty()) {
			log.error("No valid feature sets built for any product in warehouse: {}", warehouseCode);
			throw new NotFoundException("No valid data found for any product in warehouse: " + warehouseCode);
		}

		log.info("Sending {} feature sets to ML service", featureSets.size());
		ru.rtc.warehouse.ai.service.dto.MlPredictionResponse predictionResponse = predictionClient.predict(featureSets);
		log.info("Received response from ML service: {}", predictionResponse != null);

		if (predictionResponse == null || !"ok".equals(predictionResponse.getStatus())) {
			log.error("Invalid response from ML service: {}", predictionResponse);
			throw new RuntimeException("Invalid response from ML service");
		}

		processAndSavePredictions(predictionResponse, warehouse, featureSets);

		StockPredictionResponse response = convertToStockPredictionResponse(predictionResponse);

		log.info("=== PREDICTION PROCESS COMPLETED ===");
		log.info("Successfully processed {} products for warehouse: {}",
				featureSets.size(), warehouseCode);

		return response;
	}

	private StockPredictionResponse convertToStockPredictionResponse(ru.rtc.warehouse.ai.service.dto.MlPredictionResponse mlResponse) {
		List<PredictionItemDTO> predictionItems = mlResponse.getPrediction().stream()
				.map(item -> PredictionItemDTO.builder()
						.daysUntilStockout(item.getDaysUntilStockout())
						.recommendedOrder(item.getRecommendedOrder())
						.criticalLevel(item.getCriticalLevel())
						.sku(item.getSku())
						.build())
				.collect(Collectors.toList());

		return StockPredictionResponse.builder()
				.status(mlResponse.getStatus())
				.prediction(predictionItems)
				.build();
	}

	private List<String> getAllProductsForWarehouse(String warehouseCode) {
		try {
			List<Product> products = productEntityService.findAllByWarehouseCode(warehouseCode);
			List<String> skus = products.stream()
					.map(Product::getSkuCode)
					.collect(Collectors.toList());

			log.info("Found {} products for warehouse {}: {}",
					skus.size(), warehouseCode, skus);
			return skus;
		} catch (Exception e) {
			log.error("Failed to get products for warehouse: {}", warehouseCode, e);
			return Collections.emptyList();
		}
	}

	private List<MlPredictionRequest> buildFeatureSets(List<String> skus, String warehouseCode) {
		List<MlPredictionRequest> featureSets = new ArrayList<>();
		int successCount = 0;
		int failureCount = 0;

		for (String sku : skus) {
			try {
				Optional<MlPredictionRequest> featureSetOpt = buildFeatureSet(sku, warehouseCode);
				if (featureSetOpt.isPresent()) {
					featureSets.add(featureSetOpt.get());
					successCount++;
				} else {
					failureCount++;
				}
			} catch (Exception e) {
				log.warn("Failed to build feature set for SKU: {} in warehouse: {}", sku, warehouseCode, e);
				failureCount++;
			}
		}

		log.info("Feature sets built: {} successful, {} failed", successCount, failureCount);
		return featureSets;
	}

	private Optional<MlPredictionRequest> buildFeatureSet(String sku, String warehouseCode) {
		try {
			log.debug("Building feature set for SKU: {}, warehouse: {}", sku, warehouseCode);

			var inventoryData = inventoryHistoryEntAdapter.findLatestInventoryData(sku, warehouseCode);
			if (inventoryData == null) {
				log.debug("No inventory data found for SKU: {} in warehouse: {}", sku, warehouseCode);
				return Optional.empty();
			}

			var productWarehouseData = inventoryHistoryEntAdapter.findProductWarehouseData(sku, warehouseCode);
			if (productWarehouseData == null) {
				log.debug("No product warehouse data found for SKU: {} in warehouse: {}", sku, warehouseCode);
				return Optional.empty();
			}

			MlPredictionRequest featureSet = MlPredictionRequest.builder()
					.sku(sku)
					.quantity(inventoryData.getQuantity().doubleValue())
					.expectedQuantity(inventoryData.getExpectedQuantity().doubleValue())
					.difference(inventoryData.getDifference().doubleValue())
					.minStock(productWarehouseData.getMinStock().doubleValue())
					.optimalStock(productWarehouseData.getOptimalStock().doubleValue())
					.build();

			log.debug("Successfully built feature set for SKU: {}", sku);
			return Optional.of(featureSet);

		} catch (NotFoundException e) {
			log.debug("Data not found for SKU: {} in warehouse: {} - {}", sku, warehouseCode, e.getMessage());
			return Optional.empty();
		} catch (Exception e) {
			log.error("Error building feature set for SKU: {} in warehouse: {}", sku, warehouseCode, e);
			return Optional.empty();
		}
	}

	private void processAndSavePredictions(ru.rtc.warehouse.ai.service.dto.MlPredictionResponse predictionResponse, Warehouse warehouse,
										   List<MlPredictionRequest> originalFeatureSets) {
		List<MlPredictionItem> predictions = predictionResponse.getPrediction();
		log.info("Processing {} predictions from ML service", predictions.size());

		List<AiPrediction> aiPredictions = new ArrayList<>();
		Map<String, RedisPredictionDTO> redisData = new HashMap<>();

		Map<String, MlPredictionRequest> featureSetBySku = originalFeatureSets.stream()
				.collect(Collectors.toMap(
						MlPredictionRequest::getSku,
						fs -> fs
				));

		for (MlPredictionItem prediction : predictions) {
			String sku = prediction.getSku();
			Double daysUntilStockout = prediction.getDaysUntilStockout();
			Double recommendedOrder = prediction.getRecommendedOrder();
			String criticalLevel = prediction.getCriticalLevel();

			log.debug("Processing prediction for SKU {}: days_until_stockout={}, critical_level={}",
					sku, daysUntilStockout, criticalLevel);

			try {
				Product product = productEntityService.findBySkuCode(sku);

				Integer daysUntilStockoutInt = convertToInteger(daysUntilStockout);
				Integer recommendedOrderInt = convertToInteger(recommendedOrder);

				AiPrediction aiPrediction = AiPrediction.builder()
						.product(product)
						.warehouse(warehouse)
						.predictionDate(LocalDate.now())
						.daysUntilStockout(daysUntilStockoutInt)
						.recommendedOrder(recommendedOrderInt)
						.confidenceScore(CONFIDENCE_SCORE)
						.build();

				aiPredictions.add(aiPrediction);

				MlPredictionRequest originalFeatures = featureSetBySku.get(sku);

				RedisPredictionDTO predictionDTO = RedisPredictionDTO.builder()
						.sku(sku)
						.daysUntilStockout(daysUntilStockoutInt)
						.recommendedOrder(recommendedOrderInt)
						.criticalLevel(criticalLevel)
						.confidenceScore(CONFIDENCE_SCORE)
						.warehouseCode(warehouse.getCode())
						.lastUpdated(System.currentTimeMillis())
						.quantity(originalFeatures != null ? originalFeatures.getQuantity() : null)
						.expectedQuantity(originalFeatures != null ? originalFeatures.getExpectedQuantity() : null)
						.difference(originalFeatures != null ? originalFeatures.getDifference() : null)
						.minStock(originalFeatures != null ? originalFeatures.getMinStock() : null)
						.optimalStock(originalFeatures != null ? originalFeatures.getOptimalStock() : null)
						.build();

				redisData.put(sku, predictionDTO);
				log.debug("Added prediction data for SKU: {}", sku);

			} catch (NotFoundException e) {
				log.error("Product not found for SKU: {}", sku);
			} catch (Exception e) {
				log.error("Error processing prediction for SKU: {}", sku, e);
			}
		}

		savePredictionsToDatabase(aiPredictions, warehouse.getCode());
		predictionRedisService.savePredictions(warehouse.getCode(), redisData);
		sendPredictionsUpdate(warehouse.getCode());
	}

	private Integer convertToInteger(Double value) {
		if (value == null) {
			return null;
		}
		return (int) Math.round(value);
	}

	private void savePredictionsToDatabase(List<AiPrediction> predictions, String warehouseCode) {
		if (!predictions.isEmpty()) {
			try {
				aiPredictionRepository.saveAll(predictions);
				log.info("Saved {} predictions to database for warehouse: {}", predictions.size(), warehouseCode);
			} catch (Exception e) {
				log.error("Failed to save predictions to database for warehouse: {}", warehouseCode, e);
			}
		} else {
			log.warn("No predictions to save to database for warehouse: {}", warehouseCode);
		}
	}

	private void sendPredictionsUpdate(String warehouseCode) {
		try {
			Map<String, RedisPredictionDTO> predictions = predictionRedisService.getPredictions(warehouseCode);
			if (predictions == null) {
				log.debug("No predictions found in Redis for warehouse: {}", warehouseCode);
				return;
			}

			PredictionWebSocketDTO wsMessage = PredictionWebSocketDTO.builder()
					.type("prediction_update")
					.warehouseCode(warehouseCode)
					.data(predictions)
					.timestamp(System.currentTimeMillis())
					.build();

			messagingTemplate.convertAndSend(WS_PREDICTIONS_TOPIC, wsMessage);
			messagingTemplate.convertAndSend(WS_PREDICTIONS_TOPIC + "/" + warehouseCode, wsMessage);

			log.info("Sent WebSocket update for {} predictions in warehouse: {}",
					predictions.size(), warehouseCode);
		} catch (Exception e) {
			log.error("Failed to send WebSocket update for warehouse: {}", warehouseCode, e);
		}
	}
}