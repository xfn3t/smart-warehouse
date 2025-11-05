package ru.rtc.warehouse.robot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.rtc.warehouse.auth.model.RobotToken;
import ru.rtc.warehouse.auth.service.RobotAuthService;
import ru.rtc.warehouse.robot.controller.dto.request.RobotCreateRequest;
import ru.rtc.warehouse.robot.controller.dto.request.RobotUpdateRequest;
import ru.rtc.warehouse.robot.controller.dto.response.RobotTokenResponse;
import ru.rtc.warehouse.robot.mapper.RobotMapper;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.service.RobotService;
import ru.rtc.warehouse.robot.service.dto.RobotDTO;

import java.util.List;

@RestController
@RequestMapping("/api/robots")
@RequiredArgsConstructor
public class RobotController {

    private final RobotService robotService;
    private final RobotAuthService robotAuthService;
    private final RobotMapper robotMapper;

    @GetMapping
    public ResponseEntity<List<RobotDTO>> getAllRobots() {
        return ResponseEntity.ok(robotService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RobotDTO> getRobotById(@PathVariable Long id) {
        return ResponseEntity.ok(robotService.findById(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<RobotDTO> getRobotByCode(@PathVariable String code) {
        return ResponseEntity.ok(robotService.findByCode(code));
    }

    @GetMapping("/warehouse/{warehouseCode}")
    public ResponseEntity<List<RobotDTO>> getRobotsByWarehouse(@PathVariable String warehouseCode) {
        return ResponseEntity.ok(robotService.findAllByWarehouseCode(warehouseCode));
    }

    @PostMapping("/register")
    public ResponseEntity<RobotTokenResponse> registerRobot(@Valid @RequestBody RobotCreateRequest request) {
        robotService.save(request);
        String code = request.getCode();
        RobotDTO saved = robotService.findByCode(code);
        Robot robot = robotMapper.toEntity(saved);
        RobotToken tokenEntity = robotAuthService.createRobotToken(robot);
        return ResponseEntity.ok(new RobotTokenResponse(tokenEntity.getToken()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RobotDTO> updateRobot(@PathVariable Long id, @Valid @RequestBody RobotUpdateRequest req) {
        robotService.update(req, id);
        RobotDTO dto = robotService.findById(id);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRobot(@PathVariable Long id) {
        robotService.delete(id);
        return ResponseEntity.noContent().build();
    }
}