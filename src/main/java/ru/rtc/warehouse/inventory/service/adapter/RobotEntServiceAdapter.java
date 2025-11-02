package ru.rtc.warehouse.inventory.service.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.service.RobotEntityService;

@Service
@RequiredArgsConstructor
public class RobotEntServiceAdapter {

	private final RobotEntityService robotEntityService;

	public Robot findByCode(String code) {
		return robotEntityService.findByCode(code);
	}

}
