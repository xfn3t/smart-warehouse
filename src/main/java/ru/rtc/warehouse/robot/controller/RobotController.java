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


    @GetMapping("/{warehouseCode}")
    public ResponseEntity<?> getAllRobot(@PathVariable String warehouseCode) {
        return ResponseEntity.ok(robotService.findAllByWarehouseCode(warehouseCode));
    }

    //TODO: 
    /**
     * Create robot (manual registration).
     * Body: RobotCreateRequest.
     * Returns token string in JSON.
     */
    @PostMapping("/register")
    public ResponseEntity<RobotTokenResponse> register(@Valid @RequestBody RobotCreateRequest request) {

        robotService.save(request);

        String code = request.getCode();
        RobotDTO saved = robotService.findByCode(code);

        Robot robot = robotMapper.toEntity(saved);

        RobotToken tokenEntity = robotAuthService.createRobotToken(robot);
        return ResponseEntity.ok(new RobotTokenResponse(tokenEntity.getToken()));
    }
    

    /**
     * Update robot
     */
    @PutMapping("/{id}")
    public ResponseEntity<RobotDTO> update(@PathVariable Long id, @Valid @RequestBody RobotUpdateRequest req) {
        robotService.update(req, id);
        RobotDTO dto = robotService.findById(id);
        return ResponseEntity.ok(dto);
    }


    /**
     * List robots
     */
    @GetMapping
    public ResponseEntity<List<RobotDTO>> list() {
        return ResponseEntity.ok(robotService.findAll());
    }

    /**
     * Get robot by id
     */
    @GetMapping("/{id}")
    public ResponseEntity<RobotDTO> getById(@PathVariable Long id) {
        RobotDTO dto = robotService.findById(id);
        return ResponseEntity.ok(dto);
    }

    /**
     * Get robot by code
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<RobotDTO> getByCode(@PathVariable String code) {
        RobotDTO dto = robotService.findByCode(code);
        return ResponseEntity.ok(dto);
    }

   
}
