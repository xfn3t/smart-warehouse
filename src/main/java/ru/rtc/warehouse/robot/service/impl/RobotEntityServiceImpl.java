package ru.rtc.warehouse.robot.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.repository.RobotRepository;
import ru.rtc.warehouse.robot.service.RobotEntityService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RobotEntityServiceImpl implements RobotEntityService {

	private final RobotRepository robotRepository;


	@Override
	public Robot save(Robot robot) {
		return robotRepository.save(robot);
	}

	@Override
	public Robot update(Robot robot) {
		return robotRepository.save(robot);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Robot> findAll() {
		return robotRepository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public Robot findById(Long id) {
		return robotRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Robot not found"));
	}

	@Override
	@Transactional(readOnly = true)
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
	@Transactional(readOnly = true)
	public List<Robot> findAllByWarehouseCode(String warehouseCode) {
		return robotRepository.findAllByWarehouseCode(warehouseCode);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Robot> findAllWithWarehouseAndLocation() {
		return robotRepository.findAllWithWarehouseAndLocation();
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
