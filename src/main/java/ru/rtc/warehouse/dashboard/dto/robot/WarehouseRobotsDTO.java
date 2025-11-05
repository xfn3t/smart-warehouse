package ru.rtc.warehouse.dashboard.dto.robot;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class WarehouseRobotsDTO {
	private String warehouse_code;
	private List<WarehouseRobotDTO> robots;
}