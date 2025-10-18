package ru.rtc.warehouse.robot.mapper;

import org.mapstruct.Mapper;
import ru.rtc.warehouse.robot.controller.dto.request.RobotCreateRequest;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.service.dto.RobotDTO;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RobotMapper {
	RobotDTO toDto(Robot entity);
	Robot toEntity(RobotCreateRequest dto);

	List<Robot> toEntityList(List<RobotDTO> dtos);
	List<RobotDTO> toDtoList(List<Robot> robots);
}