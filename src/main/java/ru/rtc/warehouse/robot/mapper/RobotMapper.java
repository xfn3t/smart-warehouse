package ru.rtc.warehouse.robot.mapper;

import org.mapstruct.*;
import ru.rtc.warehouse.robot.controller.dto.request.RobotCreateRequest;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.model.RobotStatus;
import ru.rtc.warehouse.robot.service.dto.RobotDTO;
import ru.rtc.warehouse.warehouse.mapper.WarehouseMapper;

import java.util.List;

@Mapper(
		componentModel = "spring",
		uses = {WarehouseMapper.class},
		unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface RobotMapper {

	@Mapping(target = "status", source = "status", qualifiedByName = "mapStatusToString")
	RobotDTO toDto(Robot entity);

	@Mapping(target = "status", source = "status", qualifiedByName = "mapStringToStatus")
	@Mapping(target = "isDeleted", ignore = true)
	Robot toEntity(RobotDTO dto);	
	
	@Mapping(target = "status", source = "status", qualifiedByName = "mapStringToStatus")
	@Mapping(target = "isDeleted", ignore = true)
	Robot toEntity(RobotCreateRequest dto);


	@Named("mapStatusToString")
	default String mapStatusToString(RobotStatus status) {
		return status != null ? status.getCode().toString() : null;
	}

	@Named("mapStringToStatus")
	default RobotStatus mapStringToStatus(String code) {
		if (code == null) return null;
		RobotStatus status = new RobotStatus();
		status.setCode(RobotStatus.StatusCode.valueOf(code));
		return status;
	}

	List<RobotDTO> toDtoList(List<Robot> all);
}