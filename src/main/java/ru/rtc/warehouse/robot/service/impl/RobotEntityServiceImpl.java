package ru.rtc.warehouse.robot.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.repository.RobotRepository;
import ru.rtc.warehouse.robot.service.RobotEntityService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RobotEntityServiceImpl implements RobotEntityService {

	private final RobotRepository robotRepository;
	private final InventoryHistoryRepository inventoryHistoryRepository;


	@Override
	public Robot save(Robot robot) {
		return robotRepository.save(robot);
	}

	@Override
	public Robot update(Robot robot) {
		return robotRepository.save(robot);
	}

	@Override
	public List<Robot> findAll() {
		return robotRepository.findAll();
	}

	@Override
	public Robot findById(Long id) {
		return robotRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Robot not found"));
	}

	@Override
	public Robot findByCode(String code) {
		return robotRepository.findByCode(code)
				.orElseThrow(() -> new NotFoundException("Robot not found"));
	}

	@Override
	public Integer findMaxRobotNumber() {
		return robotRepository.findMaxRobotNumber();
	}

	@Override
	public Robot saveAndFlush(Robot robot) {
		return robotRepository.saveAndFlush(robot);
	}

	@Override
	public void delete(Long id) {
		robotRepository.deleteById(id);
	}

	@Override
	public List<Robot> findAllByWarehouseCode(String warehouseCode) {
		return robotRepository.findAllByWarehouseCode(warehouseCode);
	}

	@Transactional(readOnly = true)
	public Integer getTotalRobotsCount(Long warehouseId) {
		log.info("Getting total robots count for warehouse: {}", warehouseId);

		try {
			Integer count = robotRepository.countByWarehouseIdAndIsDeletedFalse(warehouseId);
			log.info("Total robots count for warehouse {}: {}", warehouseId, count);
			return count;
		} catch (Exception e) {
			log.error("Error getting robots count for warehouse: {}", warehouseId, e);
			throw new RuntimeException("Failed to retrieve robots count", e);
		}
	}

	@Transactional(readOnly = true)
	public List<Robot> findByWarehouseId(Long warehouseId) {
		log.info("Finding robots by warehouse ID: {}", warehouseId);

		try {
			return robotRepository.findByWarehouseIdAndIsDeletedFalse(warehouseId);
		} catch (Exception e) {
			log.error("Error finding robots for warehouse: {}", warehouseId, e);
			throw new RuntimeException("Failed to retrieve robots by warehouse", e);
		}
	}

	@Transactional(readOnly = true)
	public List<Robot> findActiveRobots() {
		log.info("Finding all active robots");

		try {
			return robotRepository.findActiveRobots();
		} catch (Exception e) {
			log.error("Error finding active robots", e);
			throw new RuntimeException("Failed to retrieve active robots", e);
		}
	}

}
