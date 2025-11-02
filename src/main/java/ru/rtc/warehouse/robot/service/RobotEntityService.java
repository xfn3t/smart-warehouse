package ru.rtc.warehouse.robot.service;

import ru.rtc.warehouse.common.CrudEntityService;
import ru.rtc.warehouse.report.dto.RobotStatisticsDto;
import ru.rtc.warehouse.robot.model.Robot;

import java.util.List;

public interface RobotEntityService extends CrudEntityService<Robot, Long> {
	Robot findByCode(String code);
	Integer findMaxRobotNumber();
	Robot saveAndFlush(Robot robot);

	Integer getTotalRobotsCount(Long id);

	RobotStatisticsDto getRobotStatistics(Long warehouseId);

	List<Robot> findAllByWarehouseCode(String warehouseCode);
}
