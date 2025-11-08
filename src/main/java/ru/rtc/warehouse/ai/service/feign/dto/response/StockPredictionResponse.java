package ru.rtc.warehouse.ai.service.feign.dto.response;

import lombok.*;
import ru.rtc.warehouse.ai.service.dto.PredictionItemDTO;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPredictionResponse {
	private String status;
	private List<PredictionItemDTO> prediction;
}