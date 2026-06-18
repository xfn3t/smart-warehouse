package ru.rtc.warehouse.ai.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
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
import ru.rtc.warehouse.ai.service.dto.ForecastDayDTO;
import ru.rtc.warehouse.ai.service.dto.RedisPredictionDTO;
import ru.rtc.warehouse.ai.service.feign.PredictionClient;
import ru.rtc.warehouse.ai.service.feign.dto.request.WeekPredictRequest;
import ru.rtc.warehouse.ai.service.feign.dto.response.ForecastDay;
import ru.rtc.warehouse.ai.service.feign.dto.response.StockPredictionResponse;
import ru.rtc.warehouse.ai.service.feign.dto.response.WeekPredictionResponse;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.product.service.ProductEntityService;
import ru.rtc.warehouse.warehouse.model.Warehouse;
import ru.rtc.warehouse.warehouse.service.WarehouseEntityService;

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

    private static final String WS_PREDICTIONS_TOPIC =
        "/topic/dashboard/predictions";

    @Override
    @Transactional
    public StockPredictionResponse predictStock(
        List<String> skus,
        String warehouseCode
    ) {
        log.info("=== STARTING v1.3 PREDICTION PROCESS ===");
        log.info("Warehouse: {}, SKUs: {}", warehouseCode, skus);

        Warehouse warehouse = warehouseEntityService.findByCode(warehouseCode);
        log.info(
            "Found warehouse: {} (ID: {})",
            warehouse.getCode(),
            warehouse.getId()
        );

        List<Product> products = resolveProducts(skus, warehouseCode);
        if (products.isEmpty()) {
            throw new NotFoundException(
                "No products found for warehouse: " + warehouseCode
            );
        }
        log.info("Products to predict: {}", products.size());

        List<AiPrediction> aiPredictions = new ArrayList<>();
        Map<String, RedisPredictionDTO> redisData = new LinkedHashMap<>();
        int success = 0;
        int failed = 0;

        for (Product product : products) {
            try {
                RedisPredictionDTO dto = predictOneProduct(product, warehouse);
                redisData.put(product.getSkuCode(), dto);

                AiPrediction ap = AiPrediction.builder()
                    .product(product)
                    .warehouse(warehouse)
                    .predictionDate(LocalDate.now())
                    .daysUntilStockout(dto.getDaysUntilStockout())
                    .recommendedOrder(dto.getRecommendedOrder())
                    .confidenceScore(dto.getConfidenceScore())
                    .build();
                aiPredictions.add(ap);

                success++;
            } catch (Exception e) {
                log.warn(
                    "Prediction failed for SKU {}: {}",
                    product.getSkuCode(),
                    e.getMessage()
                );
                failed++;
            }
        }

        log.info("v1.3 predictions: {} success, {} failed", success, failed);

        savePredictionsToDatabase(aiPredictions, warehouseCode);
        predictionRedisService.savePredictions(warehouseCode, redisData);
        sendPredictionsUpdate(warehouseCode);

        log.info("=== v1.3 PREDICTION PROCESS COMPLETED ===");
        return StockPredictionResponse.builder()
            .status("ok")
            .prediction(List.of())
            .build();
    }

    // ── приватные методы ──────────────────────────────────────────────────

    private List<Product> resolveProducts(
        List<String> skus,
        String warehouseCode
    ) {
        if (skus != null && !skus.isEmpty()) {
            return skus
                .stream()
                .map(s -> {
                    try {
                        return productEntityService.findBySkuCode(s);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }
        return productEntityService.findAllByWarehouseCode(warehouseCode);
    }

    /** Вызывает /api/predict/week для одного продукта. */
    private RedisPredictionDTO predictOneProduct(
        Product product,
        Warehouse warehouse
    ) {
        WeekPredictRequest req = WeekPredictRequest.builder()
            .productId(product.getId())
            .build();

        WeekPredictionResponse resp = predictionClient.predictWeek(req);

        if (
            resp == null ||
            !"ok".equals(resp.getStatus()) ||
            resp.getForecast() == null
        ) {
            throw new RuntimeException(
                "Empty or failed v1.3 response for product " + product.getId()
            );
        }

        List<ForecastDay> forecast = resp.getForecast();
        List<ForecastDayDTO> forecastDTOs = forecast
            .stream()
            .map(f ->
                ForecastDayDTO.builder()
                    .date(f.getDate())
                    .lower(f.getLower())
                    .median(f.getMedian())
                    .upper(f.getUpper())
                    .confidence(f.getConfidence())
                    .criticality(f.getCriticality())
                    .build()
            )
            .collect(Collectors.toList());

        // Берём criticality и confidence из первого дня прогноза
        ForecastDay firstDay = forecast.get(0);
        String criticality = firstDay.getCriticality();
        BigDecimal confidence = BigDecimal.valueOf(firstDay.getConfidence());

        // days_until_stockout: считаем сколько дней до CRITICAL (median ≤ min_stock)
        int daysUntilStockout = 365;
        var invData = inventoryHistoryEntAdapter.findLatestInventoryData(
            product.getSkuCode(),
            warehouse.getCode()
        );
        var pwData = inventoryHistoryEntAdapter.findProductWarehouseData(
            product.getSkuCode(),
            warehouse.getCode()
        );
        double minStock =
            pwData != null ? pwData.getMinStock().doubleValue() : 0;

        for (int i = 0; i < forecast.size(); i++) {
            if (forecast.get(i).getMedian() <= minStock) {
                daysUntilStockout = i + 1;
                break;
            }
        }
        // Если за 7 дней не достигли min_stock, оцениваем по тренду
        if (daysUntilStockout == 365 && forecast.size() >= 2) {
            double firstMedian = forecast.get(0).getMedian();
            double lastMedian = forecast.get(forecast.size() - 1).getMedian();
            double dailyDrop = (firstMedian - lastMedian) / forecast.size();
            if (dailyDrop > 0 && firstMedian > minStock) {
                int estDays = (int) Math.ceil(
                    (firstMedian - minStock) / dailyDrop
                );
                daysUntilStockout = Math.min(estDays, 365);
            }
        }

        // recommended_order: сколько нужно чтобы достичь optimal_stock
        double optStock =
            pwData != null ? pwData.getOptimalStock().doubleValue() : 0;
        int recommendedOrder = 0;
        if (invData != null && invData.getQuantity() < optStock) {
            recommendedOrder = (int) Math.round(
                optStock - invData.getQuantity()
            );
        }

        return RedisPredictionDTO.builder()
            .sku(product.getSkuCode())
            .productId(product.getId())
            .daysUntilStockout(daysUntilStockout)
            .recommendedOrder(recommendedOrder)
            .criticalLevel(criticality)
            .confidenceScore(confidence)
            .warehouseCode(warehouse.getCode())
            .lastUpdated(System.currentTimeMillis())
            .quantity(
                invData != null ? invData.getQuantity().doubleValue() : null
            )
            .expectedQuantity(
                invData != null
                    ? invData.getExpectedQuantity().doubleValue()
                    : null
            )
            .difference(
                invData != null ? invData.getDifference().doubleValue() : null
            )
            .minStock((double) minStock)
            .optimalStock(optStock)
            .forecast(forecastDTOs)
            .build();
    }

    private void savePredictionsToDatabase(
        List<AiPrediction> predictions,
        String warehouseCode
    ) {
        if (!predictions.isEmpty()) {
            try {
                aiPredictionRepository.saveAll(predictions);
                log.info(
                    "Saved {} predictions to DB for warehouse: {}",
                    predictions.size(),
                    warehouseCode
                );
            } catch (Exception e) {
                log.error(
                    "Failed to save predictions to DB for warehouse: {}",
                    warehouseCode,
                    e
                );
            }
        }
    }

    private void sendPredictionsUpdate(String warehouseCode) {
        try {
            Map<String, RedisPredictionDTO> predictions =
                predictionRedisService.getPredictions(warehouseCode);
            if (predictions == null || predictions.isEmpty()) return;

            PredictionWebSocketDTO msg = PredictionWebSocketDTO.builder()
                .type("prediction_update")
                .warehouseCode(warehouseCode)
                .data(predictions)
                .timestamp(System.currentTimeMillis())
                .build();

            messagingTemplate.convertAndSend(WS_PREDICTIONS_TOPIC, msg);
            messagingTemplate.convertAndSend(
                WS_PREDICTIONS_TOPIC + "/" + warehouseCode,
                msg
            );
            log.info(
                "Sent WebSocket update for {} predictions in warehouse: {}",
                predictions.size(),
                warehouseCode
            );
        } catch (Exception e) {
            log.error(
                "Failed to send WebSocket update for warehouse: {}",
                warehouseCode,
                e
            );
        }
    }
}
