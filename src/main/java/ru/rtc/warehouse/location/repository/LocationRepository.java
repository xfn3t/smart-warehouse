package ru.rtc.warehouse.location.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
	List<Location> findByWarehouse(Warehouse warehouse);
	Optional<Location> findByWarehouseAndZoneAndRowAndShelf(Warehouse warehouse, Integer zone, Integer row,
			Integer shelf);
	
}
