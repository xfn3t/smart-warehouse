package ru.rtc.warehouse.robot.service.adapter;

import ru.rtc.warehouse.auth.model.RobotToken;
import ru.rtc.warehouse.robot.model.Robot;

public interface RobotAuthAdapter {
    RobotToken createRobotToken(Robot robot);
}
