package ru.rtc.warehouse.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.product.model.ProductWarehouse;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductWarehouseRepository extends JpaRepository<ProductWarehouse, Long> {
	Optional<ProductWarehouse> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

	List<ProductWarehouse> findByProductId(Long productId);

	List<ProductWarehouse> findByWarehouseId(Long warehouseId);

	@Query("SELECT pw FROM ProductWarehouse pw WHERE pw.product.id = :productId AND pw.warehouse.id = :warehouseId AND pw.isDeleted = false")
	Optional<ProductWarehouse> findActiveByProductAndWarehouse(@Param("productId") Long productId,
															   @Param("warehouseId") Long warehouseId);

	@Query("SELECT pw FROM ProductWarehouse pw WHERE pw.product.id = :productId AND pw.isDeleted = false")
	List<ProductWarehouse> findActiveByProductId(@Param("productId") Long productId);

	@Query("SELECT pw FROM ProductWarehouse pw WHERE pw.warehouse.id = :warehouseId AND pw.isDeleted = false")
	List<ProductWarehouse> findActiveByWarehouseId(@Param("warehouseId") Long warehouseId);

	@Query("SELECT pw FROM ProductWarehouse pw " +
			"JOIN pw.product p " +
			"JOIN pw.warehouse w " +
			"WHERE p.skuCode = :sku " +
			"AND w.code = :warehouseCode " +
			"AND pw.isDeleted = false")
	Optional<ProductWarehouse> findBySkuAndWarehouseCode(@Param("sku") String sku,
														 @Param("warehouseCode") String warehouseCode);
}