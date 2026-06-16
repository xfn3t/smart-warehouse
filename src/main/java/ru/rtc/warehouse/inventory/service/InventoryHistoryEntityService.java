package ru.rtc.warehouse.inventory.service;

import java.time.LocalDateTime;
import java.util.List;
import ru.rtc.warehouse.common.CrudEntityService;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;
import ru.rtc.warehouse.warehouse.model.Warehouse;

public interface InventoryHistoryEntityService
    extends CrudEntityService<InventoryHistory, Long>
{
    InventoryHistory findByProductSKU(String sku, String warehouseCode);
    List<InventoryHistory> findAllByWarehouseCodeAndProductCode(
        String warehouseCode,
        String productCode
    );

    long countByWarehouseAndScannedAtBetween(
        Warehouse warehouse,
        LocalDateTime todayStart,
        LocalDateTime todayEnd
    );

    long countByWarehouseAndStatusAndScannedAtAfter(
        Warehouse warehouse,
        InventoryHistoryStatus.InventoryHistoryStatusCode inventoryHistoryStatusCode,
        LocalDateTime last24Hours
    );

    boolean existsByZoneAndRowAndShelfAndWarehouseAndScannedAtAfter(
        Integer zone,
        Integer row,
        Integer shelf,
        Warehouse warehouse,
        LocalDateTime since
    );

    InventoryHistory findLatestBySkuAndWarehouseCode(
        String sku,
        String warehouseCode
    );
}
