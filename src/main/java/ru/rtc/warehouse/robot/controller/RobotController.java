package ru.rtc.warehouse.robot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.rtc.warehouse.common.aspect.RequiresOwnership;
import ru.rtc.warehouse.robot.controller.dto.request.RobotCreateRequest;
import ru.rtc.warehouse.robot.controller.dto.request.RobotUpdateRequest;
import ru.rtc.warehouse.robot.controller.dto.response.RobotTokenResponse;
import ru.rtc.warehouse.robot.service.RobotService;
import ru.rtc.warehouse.robot.service.dto.RobotDTO;

import java.util.List;

@RestController
@RequestMapping("/api/robots")
@RequiredArgsConstructor
public class RobotController {

    private final RobotService robotService;

    @GetMapping
    public ResponseEntity<List<RobotDTO>> getAllRobots() {
        return ResponseEntity.ok(robotService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RobotDTO> getRobotById(@PathVariable Long id) {
        return ResponseEntity.ok(robotService.findById(id));
    }

    @GetMapping("/code/{robotCode}")
    @RequiresOwnership(codeParam = "robotCode", entityType = RequiresOwnership.EntityType.ROBOT)
    public ResponseEntity<RobotDTO> getRobotByCode(@PathVariable String robotCode) {
        return ResponseEntity.ok(robotService.findByCode(robotCode));
    }

    @GetMapping("/warehouse/{warehouseCode}")
    @RequiresOwnership(codeParam = "warehouseCode", entityType = RequiresOwnership.EntityType.WAREHOUSE)
    public ResponseEntity<List<RobotDTO>> getRobotsByWarehouse(@PathVariable String warehouseCode) {
        return ResponseEntity.ok(robotService.findAllByWarehouseCode(warehouseCode));
    }

    @PostMapping("/register")
    public ResponseEntity<RobotTokenResponse> registerRobot(@Valid @RequestBody RobotCreateRequest request) {
        RobotTokenResponse response = robotService.save(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{robotCode}")
    @RequiresOwnership(codeParam = "robotCode", entityType = RequiresOwnership.EntityType.ROBOT)
    public ResponseEntity<RobotDTO> updateRobot(@PathVariable String robotCode, @Valid @RequestBody RobotUpdateRequest req) {
        return ResponseEntity.ok(robotService.update(req, robotCode));
    }

    @DeleteMapping("/{robotCode}")
    @RequiresOwnership(codeParam = "robotCode", entityType = RequiresOwnership.EntityType.ROBOT)
    public ResponseEntity<Void> deleteRobot(@PathVariable String robotCode) {
        robotService.delete(robotCode);
        return ResponseEntity.noContent().build();
    }
}