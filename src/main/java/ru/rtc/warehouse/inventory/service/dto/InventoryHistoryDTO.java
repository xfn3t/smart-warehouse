package ru.rtc.warehouse.inventory.service.dto;

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
public class InventoryHistoryDTO {
	private Long id;
	private UUID messageId;
	private WarehouseDTO warehouse;
	private RobotDTO robot;
	private ProductDTO product;
	private Integer zone;
	private Integer rowNumber;
	private Integer shelfNumber;
	private Integer expectedQuantity;
	private Integer quantity;
	private Integer difference;
	private String status;
	private LocalDateTime scannedAt;
	private LocalDateTime createdAt;
}