package ru.rtc.warehouse.ai.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.rtc.warehouse.ai.service.dto.RedisPredictionDTO;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriticalityGroupResponseDTO {
	private String status;
	private String warehouseCode;
	private Map<String, List<RedisPredictionDTO>> data;
	private Integer totalCount;
	private Integer criticalCount;
	private Integer mediumCount;
	private Integer okCount;

	@Builder.Default
	private Long timestamp = System.currentTimeMillis();
}