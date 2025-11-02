package ru.rtc.warehouse.robot.mapper;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.robot.model.RobotStatus;

@Component
public class RobotStatusReferenceMapper {

	@Named("mapRobotStatusToString")
	public String mapRobotStatusToString(RobotStatus status) {
		return status != null ? status.getCode().toString() : null;
	}

	@Named("mapStringToRobotStatus")
	public RobotStatus mapStringToRobotStatus(String code) {
		if (code == null) return null;
		return RobotStatus.builder().code(RobotStatus.StatusCode.valueOf(code)).build();
	}
}