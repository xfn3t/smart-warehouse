package ru.rtc.warehouse.inventory.repository;


import io.micrometer.common.lang.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;
import ru.rtc.warehouse.inventory.service.product.dto.LowStockProductDTO;
import ru.rtc.warehouse.inventory.service.product.dto.ProductWithLastInventoryProjection;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface InventoryHistoryRepository extends
        JpaRepository<InventoryHistory, Long>, JpaSpecificationExecutor<InventoryHistory> {

    @Override @EntityGraph(attributePaths = {"product", "robot"})
    Page<InventoryHistory> findAll(@Nullable Specification<InventoryHistory> spec, Pageable pageable);


	// Общее количество всех продуктов на складе (сумма quantity)
	@Query("SELECT COALESCE(SUM(ih.quantity), 0) FROM InventoryHistory ih " +
			"WHERE ih.warehouse.id = :warehouseId " +
			"AND ih.isDeleted = false")
	Integer getTotalProductsCountByWarehouse(@Param("warehouseId") Long warehouseId);

	// Количество уникальных SKU на складе
	@Query("SELECT COUNT(DISTINCT ih.product.id) FROM InventoryHistory ih " +
			"WHERE ih.warehouse.id = :warehouseId " +
			"AND ih.isDeleted = false " +
			"AND ih.quantity > 0")
	Integer getUniqueProductsCountByWarehouse(@Param("warehouseId") Long warehouseId);

	// Текущее количество конкретного продукта на складе
	@Query("SELECT COALESCE(SUM(ih.quantity), 0) FROM InventoryHistory ih " +
			"WHERE ih.warehouse.id = :warehouseId " +
			"AND ih.product.id = :productId " +
			"AND ih.isDeleted = false")
	Integer getProductCountByWarehouse(@Param("warehouseId") Long warehouseId,
									   @Param("productId") Long productId);

	// Последние записи инвентаризации по складу (для актуальных данных)
	@Query("SELECT ih FROM InventoryHistory ih " +
			"WHERE ih.warehouse.id = :warehouseId " +
			"AND ih.isDeleted = false " +
			"ORDER BY ih.scannedAt DESC")
	List<InventoryHistory> findLatestByWarehouse(@Param("warehouseId") Long warehouseId, Pageable pageable);

    List<InventoryHistory> findByScannedAtBetween(LocalDateTime from, LocalDateTime to);
    Optional<InventoryHistory> findTopByOrderByScannedAtDesc();

	// Средние продажи за всё время по всем product_id (усреднённые по дням)
	@Query(value = """
        SELECT 
            AVG(daily_sum) 
        FROM (
            SELECT 
                SUM(quantity) AS daily_sum
            FROM inventory_history
            GROUP BY DATE_TRUNC('day', scanned_at)
        ) t
        """, nativeQuery = true)
	Optional<BigDecimal> avgDailySales();


	@Query(value = """
        WITH recent AS (
            SELECT SUM(quantity) AS recent_sum
            FROM inventory_history
            WHERE scanned_at >= NOW() - INTERVAL '7 days'
        ),
        monthly AS (
            SELECT SUM(quantity) AS month_sum
            FROM inventory_history
            WHERE scanned_at >= NOW() - INTERVAL '30 days'
        )
        SELECT 
            COALESCE(r.recent_sum / NULLIF(m.month_sum / 4.0, 0), 1.0)
        FROM recent r, monthly m
        """, nativeQuery = true)
	Optional<BigDecimal> seasonalFactor();

	@Query("""
        SELECT ih 
        FROM InventoryHistory ih
        WHERE ih.product.id = :productId 
          AND ih.scannedAt BETWEEN :from AND :to
          AND ih.isDeleted = false
        ORDER BY ih.scannedAt ASC
    """)
	List<InventoryHistory> findByProductAndPeriod(Long productId, LocalDateTime from, LocalDateTime to);

	@Query("""
            SELECT ih
            FROM InventoryHistory ih
            WHERE ih.product.skuCode = :sku
              AND ih.isDeleted = false
              AND ih.warehouse.code = :warehouseCode
            ORDER BY ih.scannedAt DESC
            LIMIT 1
    """)
	Optional<InventoryHistory> findByProductSKU(@Param("sku") String sku, @Param("warehouseCode") String warehouseCode);

	@Query("SELECT ih FROM InventoryHistory ih WHERE ih.product.id = :productId " +
			"AND ih.isDeleted = false ORDER BY ih.scannedAt DESC LIMIT 1")
	Optional<InventoryHistory> findLatestByProductId(@Param("productId") Long productId);

	@Query(value = """
        SELECT 
            DATE(ih.scanned_at) as scan_date,
            COALESCE(SUM(ih.expected_quantity), 0) as total_expected,
            COALESCE(SUM(ih.quantity), 0) as total_actual,
            COALESCE(SUM(ih.difference), 0) as total_difference
        FROM inventory_history ih
        JOIN products p ON ih.product_id = p.id
        WHERE p.is_deleted = false
            AND ih.is_deleted = false
            AND ih.scanned_at BETWEEN :startDate AND :endDate
            AND (:warehouseId IS NULL OR p.warehouse_id = :warehouseId)
            AND (:skuCodes IS NULL OR p.sku_code IN :skuCodes)
        GROUP BY DATE(ih.scanned_at)
        ORDER BY scan_date
        """, nativeQuery = true)
	List<Object[]> findAggregatedDailyInventory(@Param("warehouseId") Long warehouseId,
												@Param("skuCodes") List<String> skuCodes,
												@Param("startDate") LocalDateTime startDate,
												@Param("endDate") LocalDateTime endDate);

	@Query("SELECT r.id, COUNT(ih) FROM InventoryHistory ih " +
			"JOIN ih.robot r " +
			"WHERE ih.scannedAt BETWEEN :startDate AND :endDate " +
			"AND ih.isDeleted = false " +
			"AND r.isDeleted = false " +
			"GROUP BY r.id")
	List<Object[]> findScanCountsByRobotAndPeriod(@Param("startDate") LocalDateTime startDate,
												  @Param("endDate") LocalDateTime endDate);


	@Query(
			value = """
    SELECT *
    FROM (
        SELECT
            p.sku_code AS productCode,
            p.name AS productName,
            p.category AS category,
            ih.expected_quantity as expectedQuantity,
            ih.quantity AS actualQuantity,
            ih.difference AS difference,
            ih.scanned_at AS lastScannedAt,
            s.code AS statusCode,
            r.robot_code AS robotCode,
            ROW_NUMBER() OVER (PARTITION BY p.id ORDER BY ih.scanned_at DESC, ih.id DESC) AS rn
        FROM products p
        JOIN inventory_history ih ON p.id = ih.product_id
        JOIN warehouses w ON w.id = ih.warehouse_id
        LEFT JOIN inventory_status s ON ih.status_id = s.id
        LEFT JOIN robots r ON ih.robot_id = r.id
        WHERE p.is_deleted = false
          AND ih.is_deleted = false
          AND w.code = :warehouseCode
          AND (
              COALESCE(:categories, ARRAY[]::varchar[]) = '{}' OR p.category = ANY(:categories)
          )
          AND (
              COALESCE(:statuses, ARRAY[]::varchar[]) = '{}' OR s.code = ANY(:statuses)
          )
          AND (
              :searchQuery IS NULL OR
              LOWER(p.sku_code) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR
              LOWER(p.name) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR
              LOWER(r.robot_code) LIKE LOWER(CONCAT('%', :searchQuery, '%'))
          )
          AND (
              COALESCE(:robots, ARRAY[]::varchar[]) = '{}' OR r.robot_code = ANY(:robots)
          )
    ) ranked
    WHERE rn = 1
    ORDER BY productCode DESC
    """,
			countQuery = """
    SELECT COUNT(*)
    FROM (
        SELECT p.id
        FROM products p
        JOIN inventory_history ih ON p.id = ih.product_id
        JOIN warehouses w ON w.id = ih.warehouse_id
        LEFT JOIN inventory_status s ON ih.status_id = s.id
        LEFT JOIN robots r ON ih.robot_id = r.id
        WHERE p.is_deleted = false
          AND ih.is_deleted = false
          AND w.code = :warehouseCode
          AND (
              COALESCE(:categories, ARRAY[]::varchar[]) = '{}' OR p.category = ANY(:categories)
          )
          AND (
              COALESCE(:statuses, ARRAY[]::varchar[]) = '{}' OR s.code = ANY(:statuses)
          )
          AND (
              :searchQuery IS NULL OR
              LOWER(p.sku_code) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR
              LOWER(p.name) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR
              LOWER(r.robot_code) LIKE LOWER(CONCAT('%', :searchQuery, '%'))
          )
          AND (
              COALESCE(:robots, ARRAY[]::varchar[]) = '{}' OR r.robot_code = ANY(:robots)
          )
        GROUP BY p.id
    ) counted
    """,
			nativeQuery = true
	)
	Page<ProductWithLastInventoryProjection> findProductsWithLastInventoryByWarehouseWithFilters(
			@Param("warehouseCode") String warehouseCode,
			@Param("categories") List<String> categories,
			@Param("statuses") List<String> statuses,
			@Param("searchQuery") String searchQuery,
			@Param("robots") List<String> robots,
			Pageable pageable
	);


	@Query("SELECT ih FROM InventoryHistory ih WHERE ih.warehouse.code=:warehouseCode AND ih.product.skuCode=:skuCode")
	List<InventoryHistory> findAllByWarehouseCodeAndProductSkuCode(@Param("warehouseCode") String warehouseCode,
																   @Param("skuCode") String skuCode);


	@Query("SELECT COUNT(ih) FROM InventoryHistory ih " +
			"WHERE ih.location = :location AND ih.warehouse = :warehouse AND ih.isDeleted = false")
	int countByLocationAndWarehouse(@Param("location") Location location,
									@Param("warehouse") Warehouse warehouse);

	@Query("""
            SELECT ih
            FROM InventoryHistory ih
            JOIN FETCH ih.product
            JOIN FETCH ih.warehouse
            LEFT JOIN FETCH ih.robot
            WHERE ih.warehouse.code = :warehouseCode
              AND ih.product.skuCode IN :productCodes
              AND ih.isDeleted = false
            ORDER BY ih.product.skuCode, ih.scannedAt DESC
    """)
	List<InventoryHistory> findAllByWarehouseCodeAndProductCodes(
			@Param("warehouseCode") String warehouseCode,
			@Param("productCodes") List<String> productCodes);


	@Query("""
        SELECT
            p.name AS productName,
            p.skuCode AS productCode,
            pw.minStock AS minStock,
            ih.quantity AS quantity,
            (pw.minStock - ih.quantity) AS replenish
        FROM InventoryHistory ih
        JOIN ih.product p
        JOIN ih.warehouse w
        JOIN ProductWarehouse pw ON pw.product = p AND pw.warehouse = w AND pw.isDeleted = false
        WHERE ih.isDeleted = false
          AND p.isDeleted = false
          AND w.code = :warehouseCode
          AND ih.quantity <= pw.minStock
    """)
	List<LowStockProductDTO> findLowStockProductsByWarehouse(@Param("warehouseCode") String warehouseCode);

   Optional<InventoryHistory> findFirstByProduct_SkuCodeAndLocationAndWarehouseOrderByScannedAtDesc(String skuCode, Location location, Warehouse warehouse);

   // последние N записей для локации
   List<InventoryHistory> findTopNByLocationAndWarehouseOrderByScannedAtDesc(Location location, Warehouse warehouse, Pageable pageable);
   // JPA не поддерживает findTopNBy... автоматически with dynamic N, поэтому используем PageRequest.of(0, N)
   List<InventoryHistory> findByLocationAndWarehouseOrderByScannedAtDesc(Location location, Warehouse warehouse, Pageable pageable);

   // последний скан для локации
   Optional<InventoryHistory> findFirstByLocationAndWarehouseOrderByScannedAtDesc(Location location, Warehouse warehouse);

   // кол-во сканов после указанного времени
   long countByLocationAndWarehouseAndScannedAtAfter(Location location, Warehouse warehouse, LocalDateTime since);

   long countByWarehouseAndScannedAtBetween(Warehouse warehouse, LocalDateTime todayStart, LocalDateTime todayEnd);

	@Query("""
        select count(ih)
        from InventoryHistory ih
        where ih.warehouse = :warehouse
          and ih.status.code = :status
          and ih.scannedAt > :scannedAt
    """)
	long countByWarehouseAndStatusAndScannedAtAfter(Warehouse warehouse, InventoryHistoryStatus.InventoryHistoryStatusCode status, LocalDateTime scannedAt);

	boolean existsByLocationAndScannedAtAfter(Location location, LocalDateTime since);

	@Query("SELECT ih FROM InventoryHistory ih " +
			"JOIN ih.product p " +
			"JOIN ih.warehouse w " +
			"WHERE p.skuCode = :sku " +
			"AND w.code = :warehouseCode " +
			"AND ih.isDeleted = false " +
			"ORDER BY ih.scannedAt DESC " +
			"LIMIT 1")
	Optional<InventoryHistory> findLatestBySkuAndWarehouseCode(@Param("sku") String sku,
															   @Param("warehouseCode") String warehouseCode);
}
