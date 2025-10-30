package ru.rtc.warehouse.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.rtc.warehouse.robot.model.RobotStatus;
import ru.rtc.warehouse.robot.service.dto.RobotStatusDTO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RobotStatusMapper {
	RobotStatusDTO toDto(RobotStatus entity);
	RobotStatus toEntity(RobotStatusDTO dto);
}