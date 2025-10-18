package ru.rtc.warehouse.robot.service.dto;

import lombok.Getter;
import lombok.Setter;
import ru.rtc.warehouse.robot.common.enums.RobotStatus;

@Getter
@Setter
public class RobotDTO {
	private String code;
	private RobotStatus status;
	private Integer batteryLevel;
	private String currentZone;
	private Integer currentRow;
	private Integer currentShelf;
}
