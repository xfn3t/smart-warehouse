package ru.rtc.warehouse.inventory.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;

import java.time.LocalDateTime;

@Getter
@Setter
public class InventoryHistoryUpdateRequest {

	@Size(max = 50)
	private String robotCode;

	@Size(max = 50)
	private String productCode;

	@Min(0)
	private Integer quantity;

	@Min(0)
	private Integer zone;

	@Min(0)
	private Integer rowNumber;

	@Min(0)
	private Integer shelfNumber;

	@Schema(implementation = InventoryHistoryStatus.InventoryHistoryStatusCode.class, example = "LOW_STOCK")
	private InventoryHistoryStatus.InventoryHistoryStatusCode status;

	private LocalDateTime scannedAt;
}
