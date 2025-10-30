package ru.rtc.warehouse.robot.mapper;

import ru.rtc.warehouse.robot.model.RobotStatus;
import ru.rtc.warehouse.robot.service.dto.RobotStatusDTO;


public interface RobotStatusMapper {
	RobotStatusDTO toDto(RobotStatus entity);
	RobotStatus toEntity(RobotStatusDTO dto);
}
