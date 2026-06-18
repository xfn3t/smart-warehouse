package ru.rtc.warehouse.ai.controller;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rtc.warehouse.ai.controller.dto.PredictionStatsResponseDTO;
import ru.rtc.warehouse.ai.controller.dto.response.CriticalityGroupResponseDTO;
import ru.rtc.warehouse.ai.controller.dto.response.PredictionResponseDTO;
import ru.rtc.warehouse.ai.service.PredictionRedisService;
import ru.rtc.warehouse.ai.service.PredictionService;
import ru.rtc.warehouse.ai.service.dto.RedisPredictionDTO;
import ru.rtc.warehouse.warehouse.service.WarehouseEntityService;

@Slf4j
@RestController
@RequestMapping("/api/{warehouseCode}/predict")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionRedisService predictionRedisService;
    private final PredictionService predictionService;
    private final WarehouseEntityService warehouseEntityService;

    @GetMapping("/criticality/{criticality}")
    public ResponseEntity<PredictionResponseDTO> getPredictionsByCriticality(
        @PathVariable String warehouseCode,
        @PathVariable String criticality
    ) {
        log.info(
            "REST: Getting {} predictions for warehouse: {}",
            criticality,
            warehouseCode
        );

        try {
            List<RedisPredictionDTO> predictions =
                predictionRedisService.getPredictionsByCriticality(
                    warehouseCode,
                    criticality
                );

            PredictionResponseDTO response = PredictionResponseDTO.builder()
                .status("ok")
                .warehouseCode(warehouseCode)
                .criticality(criticality.toUpperCase())
                .predictions(predictions)
                .count(predictions.size())
                .build();

            log.info(
                "REST: Successfully returned {} {} predictions for warehouse: {}",
                predictions.size(),
                criticality,
                warehouseCode
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error(
                "REST: Failed to get predictions for warehouse: {} and criticality: {}",
                warehouseCode,
                criticality,
                e
            );

            PredictionResponseDTO response = PredictionResponseDTO.builder()
                .status("error")
                .warehouseCode(warehouseCode)
                .criticality(criticality.toUpperCase())
                .build();

            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/criticality")
    public ResponseEntity<
        CriticalityGroupResponseDTO
    > getAllPredictionsByCriticality(@PathVariable String warehouseCode) {
        log.info(
            "REST: Getting all criticality predictions for warehouse: {}",
            warehouseCode
        );

        try {
            Map<String, List<RedisPredictionDTO>> allCriticalityData =
                predictionRedisService.getAllPredictionsGroupedByCriticality(
                    warehouseCode
                );

            int total = allCriticalityData
                .values()
                .stream()
                .mapToInt(List::size)
                .sum();

            CriticalityGroupResponseDTO response =
                CriticalityGroupResponseDTO.builder()
                    .status("ok")
                    .warehouseCode(warehouseCode)
                    .data(allCriticalityData)
                    .totalCount(total)
                    .criticalCount(
                        allCriticalityData
                            .getOrDefault("CRITICAL", List.of())
                            .size()
                    )
                    .mediumCount(
                        allCriticalityData
                            .getOrDefault("MEDIUM", List.of())
                            .size()
                    )
                    .okCount(
                        allCriticalityData.getOrDefault("OK", List.of()).size()
                    )
                    .build();

            log.info(
                "REST: Successfully returned all criticality data for warehouse: {} (total: {})",
                warehouseCode,
                total
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error(
                "REST: Failed to get all criticality predictions for warehouse: {}",
                warehouseCode,
                e
            );

            CriticalityGroupResponseDTO response =
                CriticalityGroupResponseDTO.builder()
                    .status("error")
                    .warehouseCode(warehouseCode)
                    .build();

            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<PredictionStatsResponseDTO> getPredictionStats(
        @PathVariable String warehouseCode
    ) {
        log.info(
            "REST: Getting prediction stats for warehouse: {}",
            warehouseCode
        );

        try {
            Map<String, List<RedisPredictionDTO>> allData =
                predictionRedisService.getAllPredictionsGroupedByCriticality(
                    warehouseCode
                );

            int total = allData.values().stream().mapToInt(List::size).sum();

            PredictionStatsResponseDTO response =
                PredictionStatsResponseDTO.builder()
                    .status("ok")
                    .warehouseCode(warehouseCode)
                    .totalPredictions(total)
                    .criticalCount(
                        allData.getOrDefault("CRITICAL", List.of()).size()
                    )
                    .mediumCount(
                        allData.getOrDefault("MEDIUM", List.of()).size()
                    )
                    .okCount(allData.getOrDefault("OK", List.of()).size())
                    .lastUpdated(System.currentTimeMillis())
                    .build();

            log.info(
                "REST: Successfully returned stats for warehouse: {} (total: {})",
                warehouseCode,
                total
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error(
                "REST: Failed to get prediction stats for warehouse: {}",
                warehouseCode,
                e
            );

            PredictionStatsResponseDTO response =
                PredictionStatsResponseDTO.builder()
                    .status("error")
                    .warehouseCode(warehouseCode)
                    .build();

            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Запустить предсказание для склада (вызывает ML-сервис и сохраняет в Redis).
     */
    @PostMapping("/run")
    public ResponseEntity<PredictionStatsResponseDTO> runPrediction(
        @PathVariable String warehouseCode
    ) {
        log.info("REST: Running prediction for warehouse: {}", warehouseCode);
        try {
            predictionService.predictStock(null, warehouseCode);
            log.info(
                "REST: Prediction completed for warehouse: {}",
                warehouseCode
            );
            return getPredictionStats(warehouseCode);
        } catch (Exception e) {
            log.error(
                "REST: Prediction failed for warehouse: {}",
                warehouseCode,
                e
            );
            return ResponseEntity.status(500).body(
                PredictionStatsResponseDTO.builder()
                    .status("error")
                    .warehouseCode(warehouseCode)
                    .build()
            );
        }
    }

    /**
     * Автоматический запуск предсказаний каждые 5 минут для всех складов.
     */
    @Scheduled(fixedRate = 300_000)
    public void scheduledPrediction() {
        try {
            var warehouses = warehouseEntityService.findAll();
            log.info(
                "Scheduled: running prediction for {} warehouses",
                warehouses.size()
            );
            for (var w : warehouses) {
                try {
                    predictionService.predictStock(null, w.getCode());
                } catch (Exception e) {
                    log.error(
                        "Scheduled: prediction failed for warehouse: {}",
                        w.getCode(),
                        e
                    );
                }
            }
        } catch (Exception e) {
            log.error("Scheduled: failed to enumerate warehouses", e);
        }
    }
}
