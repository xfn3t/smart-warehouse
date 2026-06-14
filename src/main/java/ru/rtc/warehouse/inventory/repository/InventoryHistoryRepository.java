package ru.rtc.warehouse.inventory.repository;

import io.micrometer.common.lang.Nullable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
import ru.rtc.warehouse.warehouse.model.Warehouse;

@Repository
public interface InventoryHistoryRepository
    extends
        JpaRepository<InventoryHistory, Long>,
        JpaSpecificationExecutor<InventoryHistory>
{
    @Override
    @EntityGraph(attributePaths = { "product", "robot" })
    Page<InventoryHistory> findAll(
        @Nullable Specification<InventoryHistory> spec,
        Pageable pageable
    );

    // Общее количество всех продуктов на складе (сумма quantity)
    @Query(
        "SELECT COALESCE(SUM(ih.quantity), 0) FROM InventoryHistory ih " +
            "WHERE ih.warehouse.id = :warehouseId " +
            "AND ih.isDeleted = false"
    )
    Integer getTotalProductsCountByWarehouse(
        @Param("warehouseId") Long warehouseId
    );

    // Количество уникальных SKU на складе
    @Query(
        "SELECT COUNT(DISTINCT ih.product.id) FROM InventoryHistory ih " +
            "WHERE ih.warehouse.id = :warehouseId " +
            "AND ih.isDeleted = false " +
            "AND ih.quantity > 0"
    )
    Integer getUniqueProductsCountByWarehouse(
        @Param("warehouseId") Long warehouseId
    );

    // Текущее количество конкретного продукта на складе
    @Query(
        "SELECT COALESCE(SUM(ih.quantity), 0) FROM InventoryHistory ih " +
            "WHERE ih.warehouse.id = :warehouseId " +
            "AND ih.product.id = :productId " +
            "AND ih.isDeleted = false"
    )
    Integer getProductCountByWarehouse(
        @Param("warehouseId") Long warehouseId,
        @Param("productId") Long productId
    );

    // Последние записи инвентаризации по складу (для актуальных данных)
    @Query(
        "SELECT ih FROM InventoryHistory ih " +
            "WHERE ih.warehouse.id = :warehouseId " +
            "AND ih.isDeleted = false " +
            "ORDER BY ih.scannedAt DESC"
    )
    List<InventoryHistory> findLatestByWarehouse(
        @Param("warehouseId") Long warehouseId,
        Pageable pageable
    );

    List<InventoryHistory> findByScannedAtBetween(
        LocalDateTime from,
        LocalDateTime to
    );
    Optional<InventoryHistory> findTopByOrderByScannedAtDesc();

    // Средние продажи за всё время по всем product_id (усреднённые по дням)
    @Query(
        value = """
        SELECT
            AVG(daily_sum)
        FROM (
            SELECT
                SUM(quantity) AS daily_sum
            FROM inventory_history
            GROUP BY DATE_TRUNC('day', scanned_at)
        ) t
        """,
        nativeQuery = true
    )
    Optional<BigDecimal> avgDailySales();

    @Query(
        value = """
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
        """,
        nativeQuery = true
    )
    Optional<BigDecimal> seasonalFactor();

    @Query(
        """
            SELECT ih
            FROM InventoryHistory ih
            WHERE ih.product.id = :productId
              AND ih.scannedAt BETWEEN :from AND :to
              AND ih.isDeleted = false
            ORDER BY ih.scannedAt ASC
        """
    )
    List<InventoryHistory> findByProductAndPeriod(
        Long productId,
        LocalDateTime from,
        LocalDateTime to
    );

    @Query(
        """
                SELECT ih
                FROM InventoryHistory ih
                WHERE ih.product.skuCode = :sku
                  AND ih.isDeleted = false
                  AND ih.warehouse.code = :warehouseCode
                ORDER BY ih.scannedAt DESC
                LIMIT 1
        """
    )
    Optional<InventoryHistory> findByProductSKU(
        @Param("sku") String sku,
        @Param("warehouseCode") String warehouseCode
    );

    @Query(
        "SELECT ih FROM InventoryHistory ih WHERE ih.product.id = :productId " +
            "AND ih.isDeleted = false ORDER BY ih.scannedAt DESC LIMIT 1"
    )
    Optional<InventoryHistory> findLatestByProductId(
        @Param("productId") Long productId
    );

    @Query(
        value = """
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
        """,
        nativeQuery = true
    )
    List<Object[]> findAggregatedDailyInventory(
        @Param("warehouseId") Long warehouseId,
        @Param("skuCodes") List<String> skuCodes,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query(
        "SELECT r.id, COUNT(ih) FROM InventoryHistory ih " +
            "JOIN ih.robot r " +
            "WHERE ih.scannedAt BETWEEN :startDate AND :endDate " +
            "AND ih.isDeleted = false " +
            "AND r.isDeleted = false " +
            "GROUP BY r.id"
    )
    List<Object[]> findScanCountsByRobotAndPeriod(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query(
        """
            SELECT ih
            FROM InventoryHistory ih
            JOIN ih.warehouse w
            JOIN ih.product p
            LEFT JOIN ih.robot r
            LEFT JOIN ih.status s
            WHERE w.code = :warehouseCode
              AND ih.isDeleted = false
              AND p.isDeleted = false
              AND (:categories IS NULL OR p.category IN :categories)
              AND (:statuses IS NULL OR s.code IN :statuses)
              AND (:searchQuery IS NULL OR :searchQuery = ''
                   OR LOWER(p.skuCode) LIKE LOWER(CONCAT('%', :searchQuery, '%'))
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :searchQuery, '%'))
                   OR LOWER(r.code) LIKE LOWER(CONCAT('%', :searchQuery, '%')))
              AND (:robots IS NULL OR r.code IN :robots)
            ORDER BY ih.scannedAt DESC
        """
    )
    Page<InventoryHistory> findAllByWarehouseWithFilters(
        @Param("warehouseCode") String warehouseCode,
        @Param("categories") List<String> categories,
        @Param("statuses") List<String> statuses,
        @Param("searchQuery") String searchQuery,
        @Param("robots") List<String> robots,
        Pageable pageable
    );

    @Query(
        "SELECT ih FROM InventoryHistory ih WHERE ih.warehouse.code=:warehouseCode AND ih.product.skuCode=:skuCode"
    )
    List<InventoryHistory> findAllByWarehouseCodeAndProductSkuCode(
        @Param("warehouseCode") String warehouseCode,
        @Param("skuCode") String skuCode
    );

    @Query(
        "SELECT COUNT(ih) FROM InventoryHistory ih " +
            "WHERE ih.zone = :zone AND ih.row = :row AND ih.shelf = :shelf AND ih.warehouse = :warehouse AND ih.isDeleted = false"
    )
    int countByZoneAndRowAndShelfAndWarehouse(
        @Param("zone") Integer zone,
        @Param("row") Integer row,
        @Param("shelf") Integer shelf,
        @Param("warehouse") Warehouse warehouse
    );

    @Query(
        """
                SELECT ih
                FROM InventoryHistory ih
                JOIN FETCH ih.product
                JOIN FETCH ih.warehouse
                LEFT JOIN FETCH ih.robot
                WHERE ih.warehouse.code = :warehouseCode
                  AND ih.product.skuCode IN :productCodes
                  AND ih.isDeleted = false
                ORDER BY ih.product.skuCode, ih.scannedAt DESC
        """
    )
    List<InventoryHistory> findAllByWarehouseCodeAndProductCodes(
        @Param("warehouseCode") String warehouseCode,
        @Param("productCodes") List<String> productCodes
    );

    @Query(
        """
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
            WHERE ih.id IN (
                SELECT MAX(ih2.id)
                FROM InventoryHistory ih2
                WHERE ih2.isDeleted = false
                GROUP BY ih2.product.id
            )
              AND ih.quantity <= pw.minStock
              AND p.isDeleted = false
              AND w.code = :warehouseCode
        """
    )
    List<LowStockProductDTO> findLowStockProductsByWarehouse(
        @Param("warehouseCode") String warehouseCode
    );

    Optional<
        InventoryHistory
    > findFirstByProduct_SkuCodeAndZoneAndRowAndShelfAndWarehouseOrderByScannedAtDesc(
        String skuCode,
        Integer zone,
        Integer row,
        Integer shelf,
        Warehouse warehouse
    );

    // последние N записей для локации (zone/row/shelf)
    List<
        InventoryHistory
    > findTopNByZoneAndRowAndShelfAndWarehouseOrderByScannedAtDesc(
        Integer zone,
        Integer row,
        Integer shelf,
        Warehouse warehouse,
        Pageable pageable
    );
    // JPA не поддерживает findTopNBy... автоматически with dynamic N, поэтому используем PageRequest.of(0, N)
    List<
        InventoryHistory
    > findByZoneAndRowAndShelfAndWarehouseOrderByScannedAtDesc(
        Integer zone,
        Integer row,
        Integer shelf,
        Warehouse warehouse,
        Pageable pageable
    );

    // последний скан для локации (zone/row/shelf)
    Optional<
        InventoryHistory
    > findFirstByZoneAndRowAndShelfAndWarehouseOrderByScannedAtDesc(
        Integer zone,
        Integer row,
        Integer shelf,
        Warehouse warehouse
    );

    // кол-во сканов после указанного времени
    long countByZoneAndRowAndShelfAndWarehouseAndScannedAtAfter(
        Integer zone,
        Integer row,
        Integer shelf,
        Warehouse warehouse,
        LocalDateTime since
    );

    long countByWarehouseAndScannedAtBetween(
        Warehouse warehouse,
        LocalDateTime todayStart,
        LocalDateTime todayEnd
    );

    @Query(
        """
            select count(ih)
            from InventoryHistory ih
            where ih.warehouse = :warehouse
              and ih.status.code = :status
              and ih.scannedAt > :scannedAt
        """
    )
    long countByWarehouseAndStatusAndScannedAtAfter(
        Warehouse warehouse,
        InventoryHistoryStatus.InventoryHistoryStatusCode status,
        LocalDateTime scannedAt
    );

    boolean existsByZoneAndRowAndShelfAndWarehouseAndScannedAtAfter(
        Integer zone,
        Integer row,
        Integer shelf,
        Warehouse warehouse,
        LocalDateTime since
    );

    @Query(
        "SELECT ih FROM InventoryHistory ih " +
            "JOIN ih.product p " +
            "JOIN ih.warehouse w " +
            "WHERE p.skuCode = :sku " +
            "AND w.code = :warehouseCode " +
            "AND ih.isDeleted = false " +
            "ORDER BY ih.scannedAt DESC " +
            "LIMIT 1"
    )
    Optional<InventoryHistory> findLatestBySkuAndWarehouseCode(
        @Param("sku") String sku,
        @Param("warehouseCode") String warehouseCode
    );

    @Query(
        value = """
        SELECT ih.*
        FROM (
        	SELECT DISTINCT ON (ih2.product_id)
        		ih2.*
        	FROM inventory_history ih2
        	WHERE ih2.warehouse_id = (SELECT id FROM warehouses WHERE code = :warehouseCode)
        	  AND ih2.is_deleted = false
        	ORDER BY ih2.product_id, ih2.scanned_at DESC
        ) ih
        ORDER BY ih.scanned_at DESC NULLS LAST
        """,
        nativeQuery = true
    )
    List<InventoryHistory> findLatestByWarehouseGroupedByProduct(
        @Param("warehouseCode") String warehouseCode
    );

    @Query(
        value = """
            SELECT
                p.sku_code AS skuCode,
                p.name AS productName,
                DATE_TRUNC(:period, ih.scanned_at) AS bucket,
                CAST(AVG(ih.quantity) AS INTEGER) AS quantity
            FROM inventory_history ih
            JOIN products p ON p.id = ih.product_id
            JOIN warehouses w ON w.id = ih.warehouse_id
            WHERE w.code = :warehouseCode
              AND p.sku_code IN (:productCodes)
              AND ih.is_deleted = FALSE
              AND (:from IS NULL OR ih.scanned_at >= CAST(:from AS TIMESTAMP))
              AND (:to IS NULL OR ih.scanned_at <= CAST(:to AS TIMESTAMP))
            GROUP BY p.sku_code, p.name, bucket
            ORDER BY p.sku_code, bucket
        """,
        nativeQuery = true
    )
    List<Object[]> findSmoothedByWarehouseAndSkus(
        @Param("warehouseCode") String warehouseCode,
        @Param("productCodes") List<String> productCodes,
        @Param("period") String period,
        @Param("from") String from,
        @Param("to") String to
    );

    @Query(
        "SELECT DISTINCT ih.zone, ih.row, ih.shelf, ih.warehouse " +
            "FROM InventoryHistory ih WHERE ih.isDeleted = false"
    )
    List<Object[]> findDistinctZoneRowShelfWithWarehouse();

    @Query(
        "SELECT DISTINCT ih.zone, ih.row, ih.shelf " +
            "FROM InventoryHistory ih WHERE ih.warehouse = :warehouse AND ih.isDeleted = false"
    )
    List<Object[]> findDistinctZoneRowShelfByWarehouse(
        @Param("warehouse") Warehouse warehouse
    );

    @Query(
        "SELECT COUNT(DISTINCT CONCAT(ih.zone, '-', ih.row, '-', ih.shelf)) " +
            "FROM InventoryHistory ih WHERE ih.warehouse = :warehouse AND ih.isDeleted = false"
    )
    long countDistinctZoneRowShelfByWarehouse(
        @Param("warehouse") Warehouse warehouse
    );
}
