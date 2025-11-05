package ru.rtc.warehouse.dashboard.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rtc.warehouse.common.aspect.RequiresOwnership;
import ru.rtc.warehouse.dashboard.dto.WarehouseStatsDTO;
import ru.rtc.warehouse.dashboard.dto.location.WarehouseLocationsDTO;
import ru.rtc.warehouse.dashboard.dto.robot.WarehouseRobotsDTO;
import ru.rtc.warehouse.dashboard.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard/warehouses")
@RequiredArgsConstructor
@RequiresOwnership(codeParam = "warehouseCode", entityType = RequiresOwnership.EntityType.WAREHOUSE)
public class WarehouseDashboardController {

	private final DashboardService dashboardService;

	@GetMapping("/{warehouseCode}/stats")
	public ResponseEntity<WarehouseStatsDTO> getWarehouseStats(@PathVariable String warehouseCode) {
		return ResponseEntity.ok(dashboardService.getWarehouseStats(warehouseCode));
	}

	@GetMapping("/{warehouseCode}/locations")
	public ResponseEntity<WarehouseLocationsDTO> getWarehouseLocations(@PathVariable String warehouseCode) {
		return ResponseEntity.ok(dashboardService.getWarehouseLocations(warehouseCode));
	}

	@GetMapping("/{warehouseCode}/robots")
	public ResponseEntity<WarehouseRobotsDTO> getWarehouseRobots(@PathVariable String warehouseCode) {
		return ResponseEntity.ok(dashboardService.getWarehouseRobots(warehouseCode));
	}
}