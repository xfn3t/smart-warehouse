package ru.rtc.warehouse.inventory.controller.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import ru.rtc.warehouse.inventory.common.InventoryHistoryStatus;

import java.time.LocalDateTime;

@Getter
@Setter
public class InventoryHistoryUpdateRequest {
	private String robotCode;
	private String productCode;
	private Integer quantity;
	@Size(min = 1, max = 10, message = "Zone must be between 1 and 10 characters")
	private String zone;
	private Integer rowNumber;
	private Integer shelfNumber;
	private InventoryHistoryStatus status;
	private LocalDateTime scannedAt;
}
