package ru.rtc.warehouse.common.aspect;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.service.RobotEntityService;
import ru.rtc.warehouse.warehouse.model.Warehouse;
import ru.rtc.warehouse.warehouse.service.WarehouseEntityService;

@Service
public class OwnershipGuard {

	private final WarehouseEntityService warehouseService;
	private final RobotEntityService robotService;

	public OwnershipGuard(WarehouseEntityService warehouseService,
						  RobotEntityService robotService) {
		this.warehouseService = warehouseService;
		this.robotService = robotService;
		System.out.println(">>> OwnershipGuard bean initialized!");
	}

	public void assertOwnership(RequiresOwnership.EntityType type, String code, Long userId) {
		System.out.println(">>> OwnershipGuard checking " + type + " with code: " + code + " for user: " + userId);

		switch (type) {
			case WAREHOUSE -> assertWarehouseOwnership(code, userId);
			case ROBOT -> assertRobotOwnership(code, userId);
			default -> throw new IllegalArgumentException("Unsupported entity type: " + type);
		}

		System.out.println(">>> Access granted for user " + userId + " to " + type + " " + code);
	}

	private void assertWarehouseOwnership(String warehouseCode, Long userId) {
		Warehouse warehouse = warehouseService.findByCode(warehouseCode);
		boolean hasAccess = warehouse.getUsers()
				.stream()
				.anyMatch(u -> u.getId().equals(userId));

		if (!hasAccess) {
			throw new AccessDeniedException("User does not have access to warehouse: " + warehouseCode);
		}
	}

	private void assertRobotOwnership(String robotCode, Long userId) {
		Robot robot = robotService.findByCode(robotCode);
		Warehouse warehouse = robot.getWarehouse();
		boolean hasAccess = warehouse.getUsers()
				.stream()
				.anyMatch(u -> u.getId().equals(userId));

		if (!hasAccess) {
			throw new AccessDeniedException("User does not have access to robot: " + robotCode);
		}
	}
}