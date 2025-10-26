package ru.rtc.warehouse.inventory.service;

import ru.rtc.warehouse.common.CrudEntityService;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;

public interface InventoryHistoryStatusService extends CrudEntityService<InventoryHistoryStatus, Long> {
	InventoryHistoryStatus findByCode(InventoryHistoryStatus.InventoryHistoryStatusCode code);
}
