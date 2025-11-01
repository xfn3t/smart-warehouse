package ru.rtc.warehouse.inventory.controller.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCsvDto {
	private String skuCode;
	private String name;
	private String category;
	private String location;
	private Integer zone;
	private Integer row;
	private Integer shelf;
	private Integer quantity;
	private Integer minStock;
	private Integer optimalStock;
}