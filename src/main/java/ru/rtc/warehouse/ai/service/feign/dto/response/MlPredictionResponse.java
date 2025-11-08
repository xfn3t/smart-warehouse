package ru.rtc.warehouse.ai.service.dto;

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
public class MlPredictionResponse {
	private String status;
	private List<MlPredictionItem> prediction;
}