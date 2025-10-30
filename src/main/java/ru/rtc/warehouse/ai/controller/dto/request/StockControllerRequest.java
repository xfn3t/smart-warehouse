package ru.rtc.warehouse.ai.controller.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockControllerRequest {
	private String category;
	private int currentStock;
	private int minStock;
	private int optimalStock;
}
