package ru.rtc.warehouse.robot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import ru.rtc.warehouse.robot.controller.dto.request.RobotDataRequest;
import ru.rtc.warehouse.robot.service.RobotDataService;
import ru.rtc.warehouse.robot.controller.dto.response.RobotDataResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/robots")
public class RobotDataController {

    private final RobotDataService robotDataService;

    @PostMapping("/data")
    @PreAuthorize("hasRole('ROBOT') and #request.code == authentication.name")
    public ResponseEntity<RobotDataResponse> receiveData(@Valid @RequestBody RobotDataRequest request) {
        RobotDataResponse resp = robotDataService.processRobotData(request);
        return ResponseEntity.ok(resp);
    }


}
