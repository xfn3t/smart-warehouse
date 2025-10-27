package ru.rtc.warehouse.inventory.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.inventory.model.InventoryHistory;

@Repository
public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Long> {
    
   Optional<InventoryHistory> findFirstByProduct_CodeAndZoneOrderByScannedAtDesc(String skuCode, String zone);

}
