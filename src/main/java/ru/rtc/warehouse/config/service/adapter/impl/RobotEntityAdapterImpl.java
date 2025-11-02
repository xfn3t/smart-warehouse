package ru.rtc.warehouse.config.service.adapter.impl;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ru.rtc.warehouse.config.service.adapter.RobotEntityAdapter;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.service.RobotEntityService;

@Component
@RequiredArgsConstructor
public class RobotEntityAdapterImpl implements RobotEntityAdapter {
    private final RobotEntityService robotEntityService;

    public Robot findByCode(String code) {
        return robotEntityService.findByCode(code);
    }
}
