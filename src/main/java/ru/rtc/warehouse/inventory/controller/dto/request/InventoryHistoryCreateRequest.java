package ru.rtc.warehouse.inventory.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import ru.rtc.warehouse.inventory.common.InventoryHistoryStatus;

import java.time.LocalDateTime;

@Getter
@Setter
public class InventoryHistoryCreateRequest {
	@NotNull(message = "Robot ID is required")
	private Long robotId;

	@NotNull(message = "Product ID is required")
	@Size(max = 50, message = "Product ID must not exceed 50 characters")
	private String productId;

	@NotNull(message = "Quantity is required")
	private Integer quantity;

	@NotNull(message = "Zone is required")
	@Size(min = 1, max = 10, message = "Zone must be between 1 and 10 characters")
	private String zone;

	private Integer rowNumber;

	private Integer shelfNumber;

	@NotNull(message = "Status is required")
	private InventoryHistoryStatus status;

	@NotNull(message = "Scanned at date is required")
	private LocalDateTime scannedAt;
}
