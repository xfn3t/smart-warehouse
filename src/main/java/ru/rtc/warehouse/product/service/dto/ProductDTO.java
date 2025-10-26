package ru.rtc.warehouse.product.service.dto;

import lombok.Getter;
import lombok.Setter;
import ru.rtc.warehouse.warehouse.service.dto.WarehouseDTO;

@Getter
@Setter
public class ProductDTO {
	private Long id;
	private String code;
	private String name;
	private String category;
	private Integer minStock;
	private Integer optimalStock;
	private Long warehouseId;
}