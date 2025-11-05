package ru.rtc.warehouse.robot.controller.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RobotCreateRequest {

	@Size(max = 50, message = "Code must not exceed 50 characters")
	@Pattern(regexp = "RB-\\d{4}", message = "Code must follow pattern RB-XXXХ")
	private String code;

	@NotNull(message = "Status is required")
	private String status;

	@Min(value = 0, message = "Battery level must be between 0 and 100")
	@Max(value = 100, message = "Battery level must be between 0 and 100")
	private Integer batteryLevel;

	@PositiveOrZero(message = "Zone must be non-negative")
	private Integer currentZone;

	@PositiveOrZero(message = "Row must be non-negative")
	private Integer currentRow;

	@PositiveOrZero(message = "Shelf must be non-negative")
	private Integer currentShelf;

	@NotNull(message = "Warehouse ID is required")
	private String warehouseCode;
}