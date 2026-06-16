package ru.rtc.warehouse.ai.controller.dto.websocket;

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
public class WebSocketMessageDTO {
	private String type;
	private String warehouseCode;
	private Object data;
	private String status;
	private String message;
	private String criticality;

	@Builder.Default
	private Long timestamp = System.currentTimeMillis();
}