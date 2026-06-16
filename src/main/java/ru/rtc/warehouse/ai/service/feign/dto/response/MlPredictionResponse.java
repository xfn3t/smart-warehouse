package ru.rtc.warehouse.ai.service.feign.dto.response;

import lombok.*;
import ru.rtc.warehouse.ai.service.dto.MlPredictionItem;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MlPredictionResponse {
	private String status;
	private List<MlPredictionItem> prediction;
}