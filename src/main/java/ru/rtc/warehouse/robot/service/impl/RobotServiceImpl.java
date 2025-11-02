package ru.rtc.warehouse.robot.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.location.repository.LocationRepository;
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
import ru.rtc.warehouse.warehouse.service.LocationServiceAdapter;

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

	private final LocationRepository locationRepository;

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

		Location location = locationRepository
					.findByWarehouseAndZoneAndRowAndShelf(
						warehouse, req.getCurrentZone(),
						req.getCurrentRow(), req.getCurrentShelf())
					.orElseThrow(() -> new NotFoundException("Location not found with provided coordinates"));;

		robot.setLocation(location);

		RobotStatus status;
		if (req.getStatus() != null) {
			status = robotStatusService.findByCode(StatusCode.from(req.getStatus()));
		} else {
			status = robotStatusService.findByCode(StatusCode.IDLE);
		}
		robot.setStatus(status);

		robot.setLastUpdate(LocalDateTime.now());

		Robot saved = robotEntityService.saveAndFlush(robot);
		robotAuthAdapter.createRobotToken(saved);
	}


	@Override
	public void update(RobotUpdateRequest updateRequest, Long id) {
		Robot robot = robotEntityService.findById(id);
		Warehouse warehouse = robot.getWarehouse();

		if (updateRequest.getCurrentZone() != null || 
			updateRequest.getCurrentRow() != null || 
			updateRequest.getCurrentShelf() != null) {
			
			Integer zone = updateRequest.getCurrentZone() != null ? 
				updateRequest.getCurrentZone() : robot.getLocation().getZone();
			Integer row = updateRequest.getCurrentRow() != null ? 
				updateRequest.getCurrentRow() : robot.getLocation().getRow();
			Integer shelf = updateRequest.getCurrentShelf() != null ? 
				updateRequest.getCurrentShelf() : robot.getLocation().getShelf();

			Location newLocation = locationRepository
				.findByWarehouseAndZoneAndRowAndShelf(warehouse, zone, row, shelf)
				.orElseThrow(() -> new NotFoundException("Location not found with new coordinates"));
			
			robot.setLocation(newLocation);
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