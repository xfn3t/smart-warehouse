package ru.rtc.warehouse.dashboard.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.rtc.warehouse.common.aspect.RequiresOwnership;
import ru.rtc.warehouse.dashboard.dto.robot.RobotDashboardDTO;
import ru.rtc.warehouse.dashboard.dto.robot.RobotScansDTO;
import ru.rtc.warehouse.dashboard.service.DashboardService;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/robots")
@RequiredArgsConstructor
@RequiresOwnership(codeParam = "robotCode", entityType = RequiresOwnership.EntityType.ROBOT)
public class RobotDashboardController {

	private final DashboardService dashboardService;

	@GetMapping
	public ResponseEntity<List<RobotDashboardDTO>> getAllRobotsForDashboard() {
		return ResponseEntity.ok(dashboardService.getAllRobotsForDashboard());
	}

	@GetMapping("/{robotCode}")
	public ResponseEntity<RobotDashboardDTO> getRobotForDashboard(@PathVariable String robotCode) {
		return ResponseEntity.ok(dashboardService.getRobotForDashboard(robotCode));
	}

	@GetMapping("/{robotCode}/scans")
	public ResponseEntity<RobotScansDTO> getRobotScans(
			@PathVariable String robotCode,
			@RequestParam(defaultValue = "10") int limit) {
		return ResponseEntity.ok(dashboardService.getRobotScans(robotCode, limit));
	}
}