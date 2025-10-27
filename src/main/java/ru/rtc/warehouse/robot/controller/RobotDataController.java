package ru.rtc.warehouse.robot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.rtc.warehouse.robot.controller.dto.request.RobotDataRequest;
import ru.rtc.warehouse.robot.service.RobotDataService;
import ru.rtc.warehouse.robot.controller.dto.response.RobotDataResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/robots")
public class RobotDataController {

    private final RobotDataService robotDataService;

    @PostMapping("/data")
    public ResponseEntity<RobotDataResponse> receiveData(@Validated @RequestBody RobotDataRequest request,
                                                         @RequestHeader(value = "Authorization", required = false) String auth) {
        // TODO: authentication check for robot token (auth header)
        RobotDataResponse resp = robotDataService.processRobotData(request);
        return ResponseEntity.ok(resp);
    }


}
