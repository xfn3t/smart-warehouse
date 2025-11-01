package ru.rtc.warehouse.inventory.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.warehouse.model.Warehouse;

@Repository
public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Long> {
    
   Optional<InventoryHistory> findFirstByProduct_CodeAndLocationAndWarehouseOrderByScannedAtDesc(String skuCode, Location location, Warehouse warehouse);

   // последние N записей для локации
   List<InventoryHistory> findTopNByLocationAndWarehouseOrderByScannedAtDesc(Location location, Warehouse warehouse, Pageable pageable);
   // JPA не поддерживает findTopNBy... автоматически with dynamic N, поэтому используем PageRequest.of(0, N)
   List<InventoryHistory> findByLocationAndWarehouseOrderByScannedAtDesc(Location location, Warehouse warehouse, Pageable pageable);

   // последний скан для локации
   Optional<InventoryHistory> findFirstByLocationAndWarehouseOrderByScannedAtDesc(Location location, Warehouse warehouse);

   // кол-во сканов после указанного времени
   long countByLocationAndWarehouseAndScannedAtAfter(Location location, Warehouse warehouse, LocalDateTime since);

}
