package ru.rtc.warehouse.robot.mapper;

import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.robot.controller.dto.request.RobotCreateRequest;
import ru.rtc.warehouse.robot.model.Robot;

@Mapper(componentModel = "spring")
public interface RobotMapper {
	RobotCreateRequest toDto(Robot entity);
	Robot toEntity(RobotCreateRequest dto);
}