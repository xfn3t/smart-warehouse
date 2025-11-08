package ru.rtc.warehouse.ai.controller.dto.websocket;

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
public class CriticalityWebSocketDTO {
	private String type;
	private String warehouseCode;
	private String criticality;
	private Map<String, List<RedisPredictionDTO>> data;

	@Builder.Default
	private Long timestamp = System.currentTimeMillis();
}