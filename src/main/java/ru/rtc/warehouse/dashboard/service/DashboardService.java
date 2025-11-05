package ru.rtc.warehouse.dashboard.service;

import ru.rtc.warehouse.dashboard.dto.*;
import ru.rtc.warehouse.dashboard.dto.location.WarehouseLocationsDTO;
import ru.rtc.warehouse.dashboard.dto.robot.RobotDashboardDTO;
import ru.rtc.warehouse.dashboard.dto.robot.RobotScansDTO;
import ru.rtc.warehouse.dashboard.dto.robot.WarehouseRobotsDTO;

import java.util.List;

public interface DashboardService {
	List<RobotDashboardDTO> getAllRobotsForDashboard();
	RobotDashboardDTO getRobotForDashboard(String robotCode);
	RobotScansDTO getRobotScans(String robotCode, int limit);
	WarehouseStatsDTO getWarehouseStats(String warehouseCode);
	WarehouseLocationsDTO getWarehouseLocations(String warehouseCode);
	WarehouseRobotsDTO getWarehouseRobots(String warehouseCode);
}