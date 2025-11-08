package ru.rtc.warehouse.ai.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionStatsResponseDTO {
	private String status;
	private String warehouseCode;
	private Integer totalPredictions;
	private Integer criticalCount;
	private Integer mediumCount;
	private Integer okCount;
	private Long lastUpdated;

	@Builder.Default
	private Long timestamp = System.currentTimeMillis();
}