package ru.rtc.warehouse.inventory.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;

import java.time.LocalDateTime;

@Getter
@Setter
public class InventoryHistoryCreateRequest {

	@NotBlank(message = "Robot code is required")
	@Size(max = 50)
	private String robotCode;

	@NotBlank(message = "Product code (SKU) is required")
	@Size(max = 50)
	private String productCode;

	@NotNull @Min(0)
	private Integer quantity;

	@Min(0)
	private Integer zone;

	@Min(0)
	private Integer rowNumber;

	@Min(0)
	private Integer shelfNumber;

	@NotNull
	@Schema(implementation = InventoryHistoryStatus.InventoryHistoryStatusCode.class, example = "OK")
	private InventoryHistoryStatus.InventoryHistoryStatusCode status;

	@NotNull
	private LocalDateTime scannedAt;
}
