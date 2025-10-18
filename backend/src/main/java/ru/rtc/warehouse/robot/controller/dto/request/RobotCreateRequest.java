package ru.rtc.warehouse.robot.controller.dto.request;

import lombok.Getter;
import lombok.Setter;
import ru.rtc.warehouse.robot.common.enums.RobotStatus;

import javax.validation.constraints.*;

@Getter
@Setter
public class RobotCreateRequest {

	@Size(max = 50, message = "ID must not exceed 50 characters")
	@Pattern(regexp = "RB-\\d{4}", message = "ID must follow pattern RB-XXXХ")
	private String id;

	@NotNull(message = "Status is required")
	private RobotStatus status = RobotStatus.ACTIVE;

	@Min(value = 0, message = "Battery level must be between 0 and 100")
	@Max(value = 100, message = "Battery level must be between 0 and 100")
	private Integer batteryLevel;

	@Size(max = 10, message = "Zone code must not exceed 10 characters")
	private String currentZone;

	@PositiveOrZero(message = "Row must be non-negative")
	private Integer currentRow;

	@PositiveOrZero(message = "Shelf must be non-negative")
	private Integer currentShelf;

}