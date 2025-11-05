package ru.rtc.warehouse.common.aspect;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.warehouse.model.Warehouse;
import ru.rtc.warehouse.warehouse.service.WarehouseEntityService;

@Service
@RequiredArgsConstructor
public class OwnershipGuard {

	private final WarehouseEntityService warehouseEntityService;

	public void assertWarehouseOwnership(String warehouseCode, Long userId) {
		Warehouse warehouse = warehouseEntityService.findByCode(warehouseCode);
		boolean hasAccess = warehouse.getUsers()
				.stream()
				.anyMatch(u -> u.getId().equals(userId));

		if (!hasAccess) {
			throw new AccessDeniedException("User does not have access to this warehouse");
		}
	}
}
