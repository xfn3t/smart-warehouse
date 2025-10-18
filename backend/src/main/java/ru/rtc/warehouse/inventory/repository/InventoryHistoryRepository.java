package ru.rtc.warehouse.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.inventory.model.InventoryHistory;

@Repository
public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Long> {
}
