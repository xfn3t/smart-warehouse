package ru.rtc.warehouse.robot.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.robot.common.enums.RobotStatus;
import ru.rtc.warehouse.robot.controller.dto.request.RobotCreateRequest;
import ru.rtc.warehouse.robot.controller.dto.request.RobotUpdateRequest;
import ru.rtc.warehouse.robot.mapper.RobotMapper;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.service.RobotEntityService;
import ru.rtc.warehouse.robot.service.RobotService;
import ru.rtc.warehouse.robot.service.dto.RobotDTO;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RobotServiceImpl implements RobotService {

	private final RobotMapper robotMapper;
	private final RobotEntityService robotEntityService;

	private String generateUniqueRobotId() {
		Integer maxNumber = robotEntityService.findMaxRobotNumber();
		int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;

		return String.format("RB-%04d", nextNumber);
	}

	public void save(RobotCreateRequest robotCreateRequest) {
		if (robotCreateRequest.getCode() == null) {
			robotCreateRequest.setCode(generateUniqueRobotId());
		}
		Robot robot = robotMapper.toEntity(robotCreateRequest);
		robotEntityService.save(robot);
	}

	@Override
	public void update(RobotUpdateRequest updateRequest, Long id) {

		Robot robot = robotEntityService.findById(id);

		String code = updateRequest.getCode();
		RobotStatus status = updateRequest.getStatus();
		Integer batteryLevel = updateRequest.getBatteryLevel();
		String currentZone = updateRequest.getCurrentZone();
		Integer currentRow = updateRequest.getCurrentRow();
		Integer currentShelf = updateRequest.getCurrentShelf();

		if (code != null) {
			robot.setCode(code);
		}
		if (status != null) {
			robot.setStatus(status);
		}
		if (batteryLevel != null) {
			robot.setBatteryLevel(batteryLevel);
		}
		if (currentZone != null) {
			robot.setCurrentZone(currentZone);
		}
		if (currentRow != null) {
			robot.setCurrentRow(currentRow);
		}
		if (currentShelf != null) {
			robot.setCurrentShelf(currentShelf);
		}

		robot.setLastUpdate(LocalDateTime.now());

		robotEntityService.update(robot);
	}

	@Override
	public List<RobotDTO> findAll() {
		return robotMapper.toDtoList(robotEntityService.findAll());
	}

	@Override
	public RobotDTO findById(Long id) {
		return robotMapper.toDto(robotEntityService.findById(id));
	}

	@Override
	public RobotDTO findByCode(String code) {
		return robotMapper.toDto(robotEntityService.findByCode(code));
	}

	@Override
	public void delete(Long id) {
		robotEntityService.delete(id);
	}

}
