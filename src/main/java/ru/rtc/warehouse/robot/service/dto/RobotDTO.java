package ru.rtc.warehouse.robot.service.dto;

import lombok.Getter;
import lombok.Setter;
import ru.rtc.warehouse.warehouse.service.dto.WarehouseDTO;

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
	private WarehouseDTO warehouse;
	private LocalDateTime lastUpdate;
}