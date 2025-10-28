package ru.rtc.warehouse.robot.common;


import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.rtc.warehouse.robot.model.RobotStatus;

@Converter(autoApply = false)
public class StatusCodeConverter implements AttributeConverter<RobotStatus.StatusCode, String> {

	@Override
	public String convertToDatabaseColumn(RobotStatus.StatusCode attribute) {
		return attribute == null ? null : attribute.name();
	}

	@Override
	public RobotStatus.StatusCode convertToEntityAttribute(String dbData) {
		return dbData == null ? null : RobotStatus.StatusCode.valueOf(dbData);
	}
}