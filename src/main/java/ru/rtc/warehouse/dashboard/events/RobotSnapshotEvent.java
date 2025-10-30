package ru.rtc.warehouse.dashboard.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import ru.rtc.warehouse.robot.model.Robot;

/**
 * Событие: обновлён снимок состояния робота (статус/заряд/позиция).
 */
@Getter
public class RobotSnapshotEvent extends ApplicationEvent {
    private final Robot robot;

    public RobotSnapshotEvent(Object source, Robot robot) {
        super(source);
        this.robot = robot;
    }
}