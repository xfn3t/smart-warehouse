package ru.rtc.warehouse.robot.service;

import com.fasterxml.jackson.core.JsonProcessingException;

import ru.rtc.warehouse.robot.controller.dto.request.RobotStatusRequest;

public interface RobotTelemetryService {
    public void publishStatus(RobotStatusRequest req) throws JsonProcessingException;
}
