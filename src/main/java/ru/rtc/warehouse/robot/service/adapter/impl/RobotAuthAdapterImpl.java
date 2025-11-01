package ru.rtc.warehouse.robot.service.adapter.impl;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.auth.model.RobotToken;
import ru.rtc.warehouse.auth.service.RobotAuthService;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.service.adapter.RobotAuthAdapter;

@Component
@RequiredArgsConstructor
public class RobotAuthAdapterImpl implements RobotAuthAdapter {

    private final RobotAuthService robotAuthService;

    @Override
    public RobotToken createRobotToken(Robot robot) {
        return robotAuthService.createRobotToken(robot);
    }
}


