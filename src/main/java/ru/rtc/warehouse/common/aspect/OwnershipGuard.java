package ru.rtc.warehouse.common.aspect;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.service.RobotEntityService;
import ru.rtc.warehouse.warehouse.model.Warehouse;
import ru.rtc.warehouse.warehouse.service.WarehouseEntityService;

@Service
@RequiredArgsConstructor
public class OwnershipGuard {

	private final WarehouseEntityService warehouseService;
	private final RobotEntityService robotService;

	public void assertOwnership(RequiresOwnership.EntityType type, String code, Long userId) {
		switch (type) {
			case WAREHOUSE -> assertWarehouseOwnership(code, userId);
			case ROBOT -> assertRobotOwnership(code, userId);
			default -> throw new IllegalArgumentException("Unsupported entity type: " + type);
		}
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