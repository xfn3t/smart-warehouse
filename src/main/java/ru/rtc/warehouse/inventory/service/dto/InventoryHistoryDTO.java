package ru.rtc.warehouse.inventory.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.rtc.warehouse.product.service.dto.ProductDTO;
import ru.rtc.warehouse.robot.service.dto.RobotDTO;
import ru.rtc.warehouse.warehouse.service.dto.WarehouseDTO;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryHistoryDTO {
	private Long id;
	private String robotCode;
	private String skuCode;
	private String productName;
	private Integer quantity;
	private Integer zone;
	private UUID messageId;
	private WarehouseDTO warehouse;
	private RobotDTO robot;
	private ProductDTO product;
	private Integer rowNumber;
	private Integer shelfNumber;
	private Integer expectedQuantity;
	private Integer difference;
	private String status;
	private LocalDateTime scannedAt;
	private LocalDateTime createdAt;
}