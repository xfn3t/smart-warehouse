package ru.rtc.warehouse.robot.service;

import ru.rtc.warehouse.common.CrudEntityService;
import ru.rtc.warehouse.robot.model.RobotStatus;

public interface RobotStatusService extends CrudEntityService<RobotStatus, Long> {
	RobotStatus findByCode(RobotStatus.StatusCode status);
}
