package ru.rtc.warehouse.ai.service.feign.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeekPredictRequest {

    @JsonProperty("product_id")
    private Long productId;
}
