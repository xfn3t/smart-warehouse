package ru.rtc.warehouse.robot.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.robot.controller.dto.request.RobotCreateRequest;
import ru.rtc.warehouse.robot.controller.dto.request.RobotUpdateRequest;
import ru.rtc.warehouse.robot.mapper.RobotMapper;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.model.RobotStatus.StatusCode;
import ru.rtc.warehouse.robot.service.RobotEntityService;
import ru.rtc.warehouse.robot.service.RobotService;
import ru.rtc.warehouse.robot.service.RobotStatusService;
import ru.rtc.warehouse.robot.service.dto.RobotDTO;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class RobotServiceImpl implements RobotService {

	private final RobotMapper robotMapper;
	private final RobotEntityService robotEntityService;
	private final RobotStatusService robotStatusService;

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
		String status = updateRequest.getStatus();
		Integer batteryLevel = updateRequest.getBatteryLevel();
		Integer currentZone = updateRequest.getCurrentZone();
		Integer currentRow = updateRequest.getCurrentRow();
		Integer currentShelf = updateRequest.getCurrentShelf();

		if (code != null) {
			robot.setCode(code);
		}
		if (status != null) {
			robot.setStatus(robotStatusService.findByCode(StatusCode.from(status)));
		} else {
			robot.setStatus(robotStatusService.findByCode(StatusCode.IDLE));
		}
		if (batteryLevel != null) {
			robot.setBatteryLevel(batteryLevel);
		}
		if (currentZone != null) {
			robot.getLocation().setZone(currentZone);
		}
		if (currentRow != null) {
			robot.getLocation().setRow(currentRow);
		}
		if (currentShelf != null) {
			robot.getLocation().setShelf(currentShelf);
		}

		robot.setLastUpdate(LocalDateTime.now());

		robotEntityService.update(robot);
	}

	@Override
	@Transactional(readOnly = true)
	public List<RobotDTO> findAll() {
		return robotMapper.toDtoList(robotEntityService.findAll());
	}

	@Override
	@Transactional(readOnly = true)
	public RobotDTO findById(Long id) {
		return robotMapper.toDto(robotEntityService.findById(id));
	}

	@Override
	@Transactional(readOnly = true)
	public RobotDTO findByCode(String code) {
		return robotMapper.toDto(robotEntityService.findByCode(code));
	}

	@Override
	public void delete(Long id) {
		robotEntityService.delete(id);
	}

}
