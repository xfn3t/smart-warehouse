package ru.rtc.warehouse.ai.service.feign.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeekPredictionResponse {

    private String status;

    @JsonProperty("product_id")
    private Long productId;

    private List<ForecastDay> forecast;
}
