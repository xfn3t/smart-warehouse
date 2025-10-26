package ru.rtc.warehouse.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;

import java.util.Optional;

@Repository
public interface InventoryHistoryStatusRepository extends JpaRepository<InventoryHistoryStatus, Long> {
	Optional<InventoryHistoryStatus> findByCode(InventoryHistoryStatus.InventoryHistoryStatusCode code);
}
