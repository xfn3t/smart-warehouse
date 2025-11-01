package ru.rtc.warehouse.robot.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.robot.controller.dto.request.RobotCreateRequest;
import ru.rtc.warehouse.robot.controller.dto.request.RobotUpdateRequest;
import ru.rtc.warehouse.robot.mapper.RobotMapper;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.model.RobotStatus;
import ru.rtc.warehouse.robot.model.RobotStatus.StatusCode;
import ru.rtc.warehouse.robot.service.RobotEntityService;
import ru.rtc.warehouse.robot.service.RobotService;
import ru.rtc.warehouse.robot.service.RobotStatusService;
import ru.rtc.warehouse.robot.service.adapter.RobotAuthAdapter;
import ru.rtc.warehouse.robot.service.adapter.WarehouseAdapter;
import ru.rtc.warehouse.robot.service.dto.RobotDTO;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class RobotServiceImpl implements RobotService {

	private final RobotMapper robotMapper;
	private final RobotEntityService robotEntityService;
	private final RobotStatusService robotStatusService;

	private final WarehouseAdapter warehouseAdapter;
    private final RobotAuthAdapter robotAuthAdapter;

	private String generateUniqueRobotId() {
		Integer maxNumber = robotEntityService.findMaxRobotNumber();
		int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
		return String.format("RB-%04d", nextNumber);
	}

	@Override
	@Transactional
	public void save(RobotCreateRequest req) {
		if (req.getCode() == null) {
			req.setCode(generateUniqueRobotId());
		}

		Robot robot = robotMapper.toEntity(req);

		Warehouse warehouse = warehouseAdapter.findById(req.getWarehouseId());
		robot.setWarehouse(warehouse);

		Location location = new Location();
		location.setZone(req.getCurrentZone());
		location.setRow(req.getCurrentRow());
		location.setShelf(req.getCurrentShelf());
		location.setWarehouse(warehouse);
		robot.setLocation(location);

		// гарантируем, что status — managed entity из БД
		RobotStatus status;
		if (req.getStatus() != null) {
			status = robotStatusService.findByCode(StatusCode.from(req.getStatus()));
		} else {
			status = robotStatusService.findByCode(StatusCode.IDLE);
		}
		robot.setStatus(status);

		robot.setLastUpdate(LocalDateTime.now());

		Robot saved = robotEntityService.saveAndFlush(robot); // должен возвращать сохранённый Robot
		robotAuthAdapter.createRobotToken(saved);
	}


	@Override
	public void update(RobotUpdateRequest updateRequest, Long id) {
		Robot robot = robotEntityService.findById(id);

		if (updateRequest.getCode() != null) {
			robot.setCode(updateRequest.getCode());
		}
		if (updateRequest.getStatus() != null) {
			robot.setStatus(robotStatusService.findByCode(StatusCode.from(updateRequest.getStatus())));
		}
		if (updateRequest.getBatteryLevel() != null) {
			robot.setBatteryLevel(updateRequest.getBatteryLevel());
		}
		if (updateRequest.getCurrentZone() != null) {
			robot.getLocation().setZone(updateRequest.getCurrentZone());
		}
		if (updateRequest.getCurrentRow() != null) {
			robot.getLocation().setRow(updateRequest.getCurrentRow());
		}
		if (updateRequest.getCurrentShelf() != null) {
			robot.getLocation().setShelf(updateRequest.getCurrentShelf());
		}
		if (updateRequest.getWarehouseId() != null) {
			Warehouse warehouse = warehouseAdapter.findById(updateRequest.getWarehouseId());
			robot.setWarehouse(warehouse);
			// Обновляем также warehouse в location
			robot.getLocation().setWarehouse(warehouse);
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