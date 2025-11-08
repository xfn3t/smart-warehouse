package ru.rtc.warehouse.ai.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.rtc.warehouse.ai.service.dto.RedisPredictionDTO;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResponseDTO {
	private String status;
	private String warehouseCode;
	private String criticality;
	private List<RedisPredictionDTO> predictions;
	private Integer count;

	@Builder.Default
	private Long timestamp = System.currentTimeMillis();
}