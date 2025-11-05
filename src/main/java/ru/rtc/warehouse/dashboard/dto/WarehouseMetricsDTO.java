package ru.rtc.warehouse.dashboard.dto;

import lombok.Getter;
import lombok.Setter;
import ru.rtc.warehouse.dashboard.dto.robot.BatteryLevelsDTO;

import java.util.Map;

@Getter
@Setter
public class WarehouseMetricsDTO {
	private Integer total_robots;
	private Integer active_robots;
	private Integer charging_robots;
	private Integer error_robots;
	private Integer total_scans_today;
	private Integer low_stock_alerts;
	private Integer out_of_stock_alerts;
	private String total_capacity_used;
	private BatteryLevelsDTO battery_levels;
	private Map<String, Integer> robot_status_distribution;
}