package ru.rtc.warehouse.product.service.dto;

import lombok.Getter;
import lombok.Setter;
import ru.rtc.warehouse.warehouse.service.dto.WarehouseDTO;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProductWarehouseDTO {
	private Long id;
	private WarehouseDTO warehouse;
	private Integer minStock;
	private Integer optimalStock;
	private LocalDateTime createdAt;
}