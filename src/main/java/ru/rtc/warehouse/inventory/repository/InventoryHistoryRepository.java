package ru.rtc.warehouse.inventory.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.warehouse.model.Warehouse;

@Repository
public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Long> {
    
   Optional<InventoryHistory> findFirstByProduct_CodeAndZoneAndWarehouseOrderByScannedAtDesc(String skuCode, Integer zone, Warehouse warehouse);

}
