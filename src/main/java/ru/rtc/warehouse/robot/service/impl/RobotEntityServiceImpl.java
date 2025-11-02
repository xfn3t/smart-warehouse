package ru.rtc.warehouse.robot.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.inventory.repository.InventoryHistoryRepository;
import ru.rtc.warehouse.report.dto.RobotScanDto;
import ru.rtc.warehouse.report.dto.RobotStatisticsDto;
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

	@Transactional(readOnly = true)
	public RobotStatisticsDto getRobotStatistics(Long warehouseId) {
		log.info("Getting robot statistics for warehouse: {}", warehouseId);

		try {
			RobotStatisticsDto stats = new RobotStatisticsDto();

			List<Robot> robots = robotRepository.findByWarehouseIdAndIsDeletedFalse(warehouseId);

			stats.setTotalRobots(robots.size());

			long activeRobotsCount = robots.stream()
					.filter(robot -> "WORKING".equals(robot.getStatus().getCode()))
					.count();
			stats.setActiveRobots((int) activeRobotsCount);
			stats.setInactiveRobots(stats.getTotalRobots() - stats.getActiveRobots());

			stats.setEfficiency(calculateEfficiency(stats));

			LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
			List<Object[]> scanCounts = inventoryHistoryRepository.findScanCountsByRobotAndPeriod(
					weekAgo, LocalDateTime.now());

			stats.setScansPerRobot(createRobotScanData(robots, scanCounts));

			log.info("Robot statistics retrieved: total={}, active={}, efficiency={}%",
					stats.getTotalRobots(), stats.getActiveRobots(), stats.getEfficiency());

			return stats;

		} catch (Exception e) {
			log.error("Error getting robot statistics for warehouse: {}", warehouseId, e);
			throw new RuntimeException("Failed to retrieve robot statistics", e);
		}
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

	private Double calculateEfficiency(RobotStatisticsDto stats) {
		if (stats.getTotalRobots() == 0) {
			return 0.0;
		}
		return Math.round((double) stats.getActiveRobots() / stats.getTotalRobots() * 100 * 100.0) / 100.0;
	}

	private List<RobotScanDto> createRobotScanData(List<Robot> robots, List<Object[]> scanCounts) {
		return robots.stream().map(robot -> {
			RobotScanDto dto = new RobotScanDto();
			dto.setRobotCode(robot.getCode());
			dto.setStatus(robot.getStatus().getCode().toString());
			dto.setBatteryLevel(robot.getBatteryLevel());
			dto.setLastUpdate(robot.getLastUpdate());

			Long scanCount = scanCounts.stream()
					.filter(count -> count[0].equals(robot.getId()))
					.findFirst()
					.map(count -> (Long) count[1])
					.orElse(0L);

			dto.setScanCount(scanCount);
			return dto;
		}).collect(Collectors.toList());
	}
}
