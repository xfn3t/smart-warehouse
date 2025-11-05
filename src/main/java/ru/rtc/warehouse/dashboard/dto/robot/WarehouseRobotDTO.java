package ru.rtc.warehouse.dashboard.dto.robot;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class WarehouseRobotDTO {
	private String robot_id;
	private String status;
	private Integer battery_level;
	private Integer zone;
	private Integer row;
	private Integer shelf;
	private LocalDateTime last_update;
}