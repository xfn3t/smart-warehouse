package ru.rtc.warehouse.warehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
	Optional<Warehouse> findByCode(String code);
}