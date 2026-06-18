package ru.rtc.warehouse.ai.service.feign;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.rtc.warehouse.ai.service.feign.dto.request.MlPredictionRequest;
import ru.rtc.warehouse.ai.service.feign.dto.request.WeekPredictRequest;
import ru.rtc.warehouse.ai.service.feign.dto.response.MlPredictionResponse;
import ru.rtc.warehouse.ai.service.feign.dto.response.WeekPredictionResponse;

@FeignClient(name = "predictionClient", url = "${ml.api.url}")
public interface PredictionClient {
    /** Старый контракт v1.2 (без моделей). */
    @PostMapping("/api/predict")
    MlPredictionResponse predict(
        @RequestBody List<MlPredictionRequest> features
    );

    /** Новый контракт v1.3: product_id → 7-дневный прогноз с квантилями. */
    @PostMapping("/api/predict/week")
    WeekPredictionResponse predictWeek(@RequestBody WeekPredictRequest request);
}
