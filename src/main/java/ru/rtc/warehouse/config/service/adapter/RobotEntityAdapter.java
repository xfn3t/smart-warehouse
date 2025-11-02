package ru.rtc.warehouse.config.service.adapter;

import ru.rtc.warehouse.robot.model.Robot;

public interface RobotEntityAdapter {
    public Robot findByCode(String code);
}
