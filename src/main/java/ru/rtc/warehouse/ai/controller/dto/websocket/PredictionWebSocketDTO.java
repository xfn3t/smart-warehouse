package ru.rtc.warehouse.ai.controller.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.rtc.warehouse.ai.service.dto.RedisPredictionDTO;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionWebSocketDTO {
	private String type;
	private String warehouseCode;
	private Map<String, RedisPredictionDTO> data;

	@Builder.Default
	private Long timestamp = System.currentTimeMillis();
}