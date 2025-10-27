package ru.rtc.warehouse.robot.controller.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import ru.rtc.warehouse.robot.common.enums.RobotStatus;


@Getter
@Setter
public class RobotUpdateRequest {

	@Size(max = 50, message = "Code must not exceed 50 characters")
	@Pattern(regexp = "RB-\\d{4}", message = "Code must follow pattern RB-XXXХ")
	private String code;

	private RobotStatus status;

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
