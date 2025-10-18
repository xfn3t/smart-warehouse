package ru.rtc.warehouse.robot.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.robot.controller.dto.request.RobotCreateRequest;
import ru.rtc.warehouse.robot.mapper.RobotMapper;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.repository.RobotRepository;
import ru.rtc.warehouse.robot.service.RobotService;

@Service
@RequiredArgsConstructor
public class RobotServiceImpl implements RobotService {

	private final RobotMapper robotMapper;
	private final RobotRepository robotRepository;

	private String generateUniqueRobotId() {
		Integer maxNumber = robotRepository.findMaxRobotNumber();
		int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;

		return String.format("RB-%04d", nextNumber);
	}

	public void save(RobotCreateRequest robotCreateRequest) {
		if (robotCreateRequest.getCode() == null) {
			robotCreateRequest.setCode(generateUniqueRobotId());
		}
		Robot robot = robotMapper.toEntity(robotCreateRequest);
		robotRepository.save(robot);
	}
}
