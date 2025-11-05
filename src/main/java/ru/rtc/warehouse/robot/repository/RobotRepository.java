package ru.rtc.warehouse.robot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.robot.model.Robot;

import java.util.List;
import java.util.Optional;

@Repository
public interface RobotRepository extends JpaRepository<Robot, Long> {

	Optional<Robot> findByCode(String code);

	@Query("SELECT MAX(CAST(SUBSTRING(r.code, 4) AS int)) FROM Robot r WHERE r.code LIKE 'RB-%'")
	Integer findMaxRobotNumber();

	@Query("SELECT r FROM Robot r WHERE r.warehouse.id = :warehouseId AND r.isDeleted = false")
	List<Robot> findByWarehouseIdAndIsDeletedFalse(@Param("warehouseId") Long warehouseId);

	@Query("SELECT COUNT(r) FROM Robot r WHERE r.warehouse.id = :warehouseId AND r.isDeleted = false")
	Integer countByWarehouseIdAndIsDeletedFalse(@Param("warehouseId") Long warehouseId);

	@Query("SELECT r FROM Robot r WHERE r.status.code = 'WORKING' AND r.isDeleted = false")
	List<Robot> findActiveRobots();

	@Query("SELECT r FROM Robot r WHERE r.warehouse.code = :warehouseCode AND r.isDeleted = false")
	List<Robot> findAllByWarehouseCode(String warehouseCode);

	@Query("SELECT r FROM Robot r JOIN FETCH r.warehouse JOIN FETCH r.location WHERE r.isDeleted = false")
	List<Robot> findAllWithWarehouseAndLocation();
}
