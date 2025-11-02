package ru.rtc.warehouse.robot.service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RobotDTO {
	private Long id;
	private String code;
	private String status;
	private Integer batteryLevel;
	private String currentZone;
	private Integer currentRow;
	private Integer currentShelf;
	private Long warehouseId;
	private LocalDateTime lastUpdate;
}