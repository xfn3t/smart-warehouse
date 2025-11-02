package ru.rtc.warehouse.robot.service;

import ru.rtc.warehouse.robot.controller.dto.request.RobotDataRequest;
import ru.rtc.warehouse.robot.controller.dto.response.RobotDataResponse;

public interface RobotDataService {
    public RobotDataResponse processRobotData(RobotDataRequest robotDataRequest);
}
