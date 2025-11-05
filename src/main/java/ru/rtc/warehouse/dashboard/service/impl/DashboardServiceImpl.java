package ru.rtc.warehouse.dashboard.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.dashboard.dto.*;
import ru.rtc.warehouse.dashboard.dto.location.WarehouseLocationsDTO;
import ru.rtc.warehouse.dashboard.dto.robot.*;
import ru.rtc.warehouse.dashboard.service.DashboardService;
import ru.rtc.warehouse.dashboard.service.dto.AlertStatsDTO;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.service.InventoryHistoryEntityService;
import ru.rtc.warehouse.location.dto.LocationMetricsDTO;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.location.service.LocationEntityService;
import ru.rtc.warehouse.location.service.LocationMetricsService;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.service.RobotEntityService;
import ru.rtc.warehouse.warehouse.model.Warehouse;
import ru.rtc.warehouse.warehouse.service.WarehouseEntityService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

	private final RobotEntityService robotEntityService;
	private final WarehouseEntityService warehouseEntityService;
	private final InventoryHistoryEntityService inventoryHistoryEntityService;
	private final LocationMetricsService locationMetricsService;
	private final LocationEntityService locationEntityService;

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	@Override
	public List<RobotDashboardDTO> getAllRobotsForDashboard() {
		log.info("Getting all robots for dashboard");

		List<Robot> robots = robotEntityService.findAllWithWarehouseAndLocation();
		return robots.stream()
				.filter(robot -> !robot.isDeleted())
				.map(this::convertToRobotDashboardDTO)
				.collect(Collectors.toList());
	}

	@Override
	public RobotDashboardDTO getRobotForDashboard(String robotCode) {
		log.info("Getting robot {} for dashboard", robotCode);

		Robot robot = robotEntityService.findByCode(robotCode);
		if (robot.isDeleted()) {
			throw new RuntimeException("Robot is deleted: " + robotCode);
		}

		return convertToRobotDashboardDTO(robot);
	}

	@Override
	public RobotScansDTO getRobotScans(String robotCode, int limit) {
		log.info("Getting {} recent scans for robot {}", limit, robotCode);

		Robot robot = robotEntityService.findByCode(robotCode);
		List<ScanDTO> recentScans = getRecentScansFromRedis(robotCode, limit);

		RobotScansDTO response = new RobotScansDTO();
		response.setRobot_id(robotCode);
		response.setScans(recentScans);
		response.setTotal_count(recentScans.size());

		return response;
	}

	@Override
	public WarehouseStatsDTO getWarehouseStats(String warehouseCode) {
		log.info("Getting stats for warehouse {}", warehouseCode);

		Warehouse warehouse = warehouseEntityService.findByCode(warehouseCode);
		List<Robot> warehouseRobots = robotEntityService.findAllByWarehouseCode(warehouseCode);

		WarehouseStatsDTO stats = new WarehouseStatsDTO();
		stats.setWarehouse_code(warehouseCode);
		stats.setTimestamp(LocalDateTime.now());

		WarehouseMetricsDTO metrics = calculateWarehouseMetrics(warehouse, warehouseRobots);
		stats.setMetrics(metrics);

		return stats;
	}

	@Override
	public WarehouseLocationsDTO getWarehouseLocations(String warehouseCode) {
		log.info("Getting locations for warehouse {}", warehouseCode);

		Warehouse warehouse = warehouseEntityService.findByCode(warehouseCode);
		List<Location> locations = locationEntityService.findByWarehouse(warehouse);

		List<LocationMetricsDTO> locationMetrics = locations.stream()
				.map(locationMetricsService::computeFor)
				.collect(Collectors.toList());

		WarehouseLocationsDTO response = new WarehouseLocationsDTO();
		response.setWarehouse_code(warehouseCode);
		response.setLocations(locationMetrics);

		return response;
	}

	@Override
	public WarehouseRobotsDTO getWarehouseRobots(String warehouseCode) {
		log.info("Getting robots for warehouse {}", warehouseCode);

		List<Robot> robots = robotEntityService.findAllByWarehouseCode(warehouseCode);

		WarehouseRobotsDTO response = new WarehouseRobotsDTO();
		response.setWarehouse_code(warehouseCode);

		List<WarehouseRobotDTO> robotDTOs = robots.stream()
				.filter(robot -> !robot.isDeleted())
				.map(this::convertToWarehouseRobotDTO)
				.collect(Collectors.toList());

		response.setRobots(robotDTOs);
		return response;
	}

	private RobotDashboardDTO convertToRobotDashboardDTO(Robot robot) {
		RobotDashboardDTO dto = new RobotDashboardDTO();
		dto.setRobot_id(robot.getCode());
		dto.setBattery_level(robot.getBatteryLevel());

		if (robot.getLocation() != null) {
			dto.setZone(robot.getLocation().getZone());
			dto.setRow(robot.getLocation().getRow());
			dto.setShelf(robot.getLocation().getShelf());
		}

		dto.setTimestamp(robot.getLastUpdate());
		dto.setStatus(robot.getStatus().getCode().name());

		if (robot.getWarehouse() != null) {
			dto.setWarehouse_code(robot.getWarehouse().getCode());
		}

		List<ScanDTO> recentScans = getRecentScansFromRedis(robot.getCode(), 5);
		dto.setRecent_scans(recentScans);

		return dto;
	}

	private WarehouseRobotDTO convertToWarehouseRobotDTO(Robot robot) {
		WarehouseRobotDTO dto = new WarehouseRobotDTO();
		dto.setRobot_id(robot.getCode());
		dto.setStatus(robot.getStatus().getCode().name());
		dto.setBattery_level(robot.getBatteryLevel());
		dto.setLast_update(robot.getLastUpdate());

		if (robot.getLocation() != null) {
			dto.setZone(robot.getLocation().getZone());
			dto.setRow(robot.getLocation().getRow());
			dto.setShelf(robot.getLocation().getShelf());
		}

		return dto;
	}

	private WarehouseMetricsDTO calculateWarehouseMetrics(Warehouse warehouse, List<Robot> robots) {
		WarehouseMetricsDTO metrics = new WarehouseMetricsDTO();

		List<Robot> activeRobots = robots.stream()
				.filter(robot -> !robot.isDeleted())
				.collect(Collectors.toList());

		metrics.setTotal_robots(activeRobots.size());

		long workingRobots = activeRobots.stream()
				.filter(robot -> "WORKING".equals(robot.getStatus().getCode().name()))
				.count();
		metrics.setActive_robots((int) workingRobots);

		long chargingRobots = activeRobots.stream()
				.filter(robot -> "CHARGING".equals(robot.getStatus().getCode().name()))
				.count();
		metrics.setCharging_robots((int) chargingRobots);

		long errorRobots = activeRobots.stream()
				.filter(robot -> "ERROR".equals(robot.getStatus().getCode().name()))
				.count();
		metrics.setError_robots((int) errorRobots);

		// Расчет статистики батарей
		BatteryLevelsDTO batteryLevels = calculateBatteryLevels(activeRobots);
		metrics.setBattery_levels(batteryLevels);

		// Распределение по статусам
		Map<String, Integer> statusDistribution = calculateStatusDistribution(activeRobots);
		metrics.setRobot_status_distribution(statusDistribution);

		// Статистика сканирований за сегодня
		LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
		LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

		long totalScansToday = inventoryHistoryEntityService.countByWarehouseAndScannedAtBetween(warehouse, todayStart, todayEnd);
		metrics.setTotal_scans_today((int) totalScansToday);

		// Аллерты по остаткам
		AlertStatsDTO alertStats = calculateAlertStats(warehouse);
		metrics.setLow_stock_alerts(alertStats.getLowStockAlerts());
		metrics.setOut_of_stock_alerts(alertStats.getOutOfStockAlerts());

		// Использование емкости склада
		String capacityUsed = calculateCapacityUsed(warehouse);
		metrics.setTotal_capacity_used(capacityUsed);

		return metrics;
	}

	private BatteryLevelsDTO calculateBatteryLevels(List<Robot> robots) {
		BatteryLevelsDTO batteryLevels = new BatteryLevelsDTO();

		OptionalDouble avgBattery = robots.stream()
				.mapToInt(Robot::getBatteryLevel)
				.average();
		batteryLevels.setAverage(avgBattery.isPresent() ? (int) avgBattery.getAsDouble() : 0);

		OptionalInt minBattery = robots.stream()
				.mapToInt(Robot::getBatteryLevel)
				.min();
		batteryLevels.setLowest(minBattery.isPresent() ? minBattery.getAsInt() : 0);

		OptionalInt maxBattery = robots.stream()
				.mapToInt(Robot::getBatteryLevel)
				.max();
		batteryLevels.setHighest(maxBattery.isPresent() ? maxBattery.getAsInt() : 0);

		return batteryLevels;
	}

	private Map<String, Integer> calculateStatusDistribution(List<Robot> robots) {
		return robots.stream()
				.collect(Collectors.groupingBy(
						robot -> robot.getStatus().getCode().name(),
						Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
				));
	}

	private AlertStatsDTO calculateAlertStats(Warehouse warehouse) {
		// Считаем алерты за последние 24 часа
		LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);

		long lowStockAlerts = inventoryHistoryEntityService.countByWarehouseAndStatusAndScannedAtAfter(
				warehouse, InventoryHistoryStatus.InventoryHistoryStatusCode.LOW_STOCK, last24Hours);

		long outOfStockAlerts = inventoryHistoryEntityService.countByWarehouseAndStatusAndScannedAtAfter(
				warehouse, InventoryHistoryStatus.InventoryHistoryStatusCode.CRITICAL, last24Hours);

		return new AlertStatsDTO((int) lowStockAlerts, (int) outOfStockAlerts);
	}

	private String calculateCapacityUsed(Warehouse warehouse) {
		try {
			List<Location> locations = locationEntityService.findByWarehouse(warehouse);
			long totalLocations = locations.size();

			if (totalLocations > 0) {
				// Более простая логика - считаем локации с недавними сканированиями
				LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
				long activeLocations = locations.stream()
						.filter(location -> hasRecentScans(location, last24Hours))
						.count();

				int percent = (int) ((activeLocations * 100) / totalLocations);
				return percent + "%";
			}
		} catch (Exception e) {
			log.warn("Failed to calculate capacity for warehouse {}: {}", warehouse.getCode(), e.getMessage());
		}

		return "0%";
	}

	private boolean hasRecentScans(Location location, LocalDateTime since) {
		try {
			// Используем существующий метод для проверки сканирований
			return inventoryHistoryEntityService.existsByLocationAndScannedAtAfter(location, since);
		} catch (Exception e) {
			log.debug("Error checking recent scans for location: {}", e.getMessage());
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private List<ScanDTO> getRecentScansFromRedis(String robotCode, int limit) {
		try {
			String redisKey = String.format("robot:%s:recent_scans", robotCode);
			List<String> rawScans = redisTemplate.opsForList().range(redisKey, -limit, -1);

			if (rawScans == null || rawScans.isEmpty()) {
				return new ArrayList<>();
			}

			Collections.reverse(rawScans);

			return rawScans.stream()
					.map(scanJson -> {
						try {
							Map<String, Object> scanMap = objectMapper.readValue(scanJson, Map.class);
							return convertMapToScanDTO(scanMap);
						} catch (Exception e) {
							log.warn("Failed to parse scan JSON for robot {}: {}", robotCode, e.getMessage());
							return null;
						}
					})
					.filter(Objects::nonNull)
					.collect(Collectors.toList());

		} catch (Exception e) {
			log.warn("Failed to get recent scans from Redis for robot {}: {}", robotCode, e.getMessage());
			return new ArrayList<>();
		}
	}

	private ScanDTO convertMapToScanDTO(Map<String, Object> scanMap) {
		ScanDTO scanDTO = new ScanDTO();
		scanDTO.setProductCode((String) scanMap.get("productCode"));
		scanDTO.setProductName((String) scanMap.get("productName"));

		Object quantity = scanMap.get("quantity");
		if (quantity instanceof Integer) {
			scanDTO.setQuantity((Integer) quantity);
		} else if (quantity != null) {
			scanDTO.setQuantity(Integer.valueOf(quantity.toString()));
		}

		scanDTO.setStatus((String) scanMap.get("status"));

		Object diff = scanMap.get("diff");
		if (diff instanceof Integer) {
			scanDTO.setDiff((Integer) diff);
		} else if (diff != null) {
			scanDTO.setDiff(Integer.valueOf(diff.toString()));
		}

		Object scannedAt = scanMap.get("scannedAt");
		if (scannedAt instanceof String) {
			scanDTO.setScannedAt(LocalDateTime.parse((String) scannedAt));
		}

		return scanDTO;
	}

}