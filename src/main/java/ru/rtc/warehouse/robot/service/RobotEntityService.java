package ru.rtc.warehouse.robot.service;

import ru.rtc.warehouse.common.CrudEntityService;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.service.dto.RobotDTO;

public interface RobotEntityService extends CrudEntityService<Robot, Long> {
	Robot findByCode(String code);
	Integer findMaxRobotNumber();
}
