package ru.rtc.warehouse.dashboard.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.rtc.warehouse.dashboard.dto.WarehouseStatsDTO;
import ru.rtc.warehouse.dashboard.dto.location.WarehouseLocationsDTO;
import ru.rtc.warehouse.dashboard.dto.robot.RobotDashboardDTO;
import ru.rtc.warehouse.dashboard.dto.robot.RobotScansDTO;
import ru.rtc.warehouse.dashboard.dto.robot.WarehouseRobotsDTO;
import ru.rtc.warehouse.dashboard.service.DashboardService;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

	private final DashboardService dashboardService;

	@GetMapping("/robots")
	public ResponseEntity<List<RobotDashboardDTO>> getAllRobotsForDashboard() {
		return ResponseEntity.ok(dashboardService.getAllRobotsForDashboard());
	}

	@GetMapping("/robots/{robotCode}")
	public ResponseEntity<RobotDashboardDTO> getRobotForDashboard(@PathVariable String robotCode) {
		return ResponseEntity.ok(dashboardService.getRobotForDashboard(robotCode));
	}

	@GetMapping("/robots/{robotCode}/scans")
	public ResponseEntity<RobotScansDTO> getRobotScans(
			@PathVariable String robotCode,
			@RequestParam(defaultValue = "10") int limit) {
		return ResponseEntity.ok(dashboardService.getRobotScans(robotCode, limit));
	}

	@GetMapping("/warehouses/{warehouseCode}/stats")
	public ResponseEntity<WarehouseStatsDTO> getWarehouseStats(@PathVariable String warehouseCode) {
		return ResponseEntity.ok(dashboardService.getWarehouseStats(warehouseCode));
	}

	@GetMapping("/warehouses/{warehouseCode}/locations")
	public ResponseEntity<WarehouseLocationsDTO> getWarehouseLocations(@PathVariable String warehouseCode) {
		return ResponseEntity.ok(dashboardService.getWarehouseLocations(warehouseCode));
	}

	@GetMapping("/warehouses/{warehouseCode}/robots")
	public ResponseEntity<WarehouseRobotsDTO> getWarehouseRobots(@PathVariable String warehouseCode) {
		return ResponseEntity.ok(dashboardService.getWarehouseRobots(warehouseCode));
	}
}