package ru.rtc.warehouse.inventory.controller.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class InventoryHistoryUpdateRequest {
	private String robotCode;
	private String productCode;
	private Integer quantity;
	@Size(min = 1, max = 100, message = "Zone must be between 1 and 10 characters")
	private Integer zone;
	private Integer rowNumber;
	private Integer shelfNumber;
	private String status;
	private LocalDateTime scannedAt;
	private UUID messageId;
}
