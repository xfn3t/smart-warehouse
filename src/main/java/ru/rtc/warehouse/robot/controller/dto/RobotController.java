package ru.rtc.warehouse.robot.controller.dto;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rtc.warehouse.robot.service.RobotService;

@RestController
@RequestMapping("/api/{warehouseCode}/robot")
@RequiredArgsConstructor
public class RobotController {

	private final RobotService robotService;

	@GetMapping
	public ResponseEntity<?> getAllRobot(@PathVariable String warehouseCode) {
		return ResponseEntity.ok(robotService.findAllByWarehouseCode(warehouseCode));
	}
}
