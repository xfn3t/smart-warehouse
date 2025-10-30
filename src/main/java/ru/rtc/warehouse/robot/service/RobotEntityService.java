package ru.rtc.warehouse.robot.service;

import ru.rtc.warehouse.common.CrudEntityService;
import ru.rtc.warehouse.robot.model.Robot;

public interface RobotEntityService extends CrudEntityService<Robot, Long> {
	Robot findByCode(String code);
	Integer findMaxRobotNumber();
	Robot saveAndFlush(Robot robot);
}
