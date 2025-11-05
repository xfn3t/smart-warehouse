package ru.rtc.warehouse.dashboard.service.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.service.InventoryHistoryEntityService;

import java.util.List;

@Service("dashboardInventoryHistoryEntServiceAdapter")
@RequiredArgsConstructor
public class DashboardInventoryHistoryEntServiceAdapter {

	private final InventoryHistoryEntityService inventoryHistoryService;

	public List<InventoryHistory> findAll() {
		return inventoryHistoryService.findAll();
	}
}
