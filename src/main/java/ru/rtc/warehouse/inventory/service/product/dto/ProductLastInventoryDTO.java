package ru.rtc.warehouse.inventory.service.product.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class ProductLastInventoryDTO {
	private String productCode;
	private String productName;
	private String category;
	private Integer expectedQuantity;
	private Integer actualQuantity;
	private Integer difference;
	private LocalDateTime lastScannedAt;
	private String statusCode;
	private String robotCode;
}