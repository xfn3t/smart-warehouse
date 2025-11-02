package ru.rtc.warehouse.warehouse.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
	@EntityGraph(attributePaths = {
			"products",
			"locations"
	})
	Optional<Warehouse> findByCode(String code);

	@Query(value = "SELECT w.* FROM warehouses w " +
			"JOIN user_warehouses uw ON w.id = uw.warehouse_id " +
			"WHERE uw.user_id = :userId AND w.is_deleted = false",
			nativeQuery = true)
	List<Warehouse> findByUserId(Long userId);

	Optional<Warehouse> findByCodeAndIsDeletedFalse(String warehouseCode);

	@Query("SELECT w FROM Warehouse w WHERE w.id = :id AND w.isDeleted = false")
	Optional<Warehouse> findByIdAndIsDeletedFalse(@Param("id") Long id);

	@Query("SELECT w FROM Warehouse w WHERE w.isDeleted = false")
	List<Warehouse> findAllActiveWarehouses();
}