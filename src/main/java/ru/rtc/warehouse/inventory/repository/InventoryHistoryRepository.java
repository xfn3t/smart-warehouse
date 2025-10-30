package ru.rtc.warehouse.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.inventory.model.InventoryHistory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Long> {

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


	// Сезонный коэффициент на основе последних 7 и 30 дней
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
			WHERE ih.product.code = :sku 
			  AND ih.isDeleted = false
			  AND ih.warehouse.code = :warehouseCode
			ORDER BY ih.scannedAt ASC
			""")
	Optional<InventoryHistory> findByProductSKU(String sku, String warehouseCode);
}
