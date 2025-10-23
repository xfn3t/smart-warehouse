package ru.rtc.warehouse.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.inventory.model.InventoryHistory;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Long> {

	@Query(value = """
			SELECT\s
			    product_id,
			    AVG(SUM(quantity)) OVER (PARTITION BY product_id) AS avg_daily_sales
			FROM inventory_history
			GROUP BY product_id, DATE_TRUNC('day', scanned_at)
			""", nativeQuery = true)
	Optional<BigDecimal> avgDailySales();


	@Query(value = """
			WITH recent AS (
			    SELECT product_id, SUM(quantity) AS recent_sum
			    FROM inventory_history
			    WHERE scanned_at >= NOW() - INTERVAL '7 days'
			    GROUP BY product_id
			),
			monthly AS (
			    SELECT product_id, SUM(quantity) AS month_sum
			    FROM inventory_history
			    WHERE scanned_at >= NOW() - INTERVAL '30 days'
			    GROUP BY product_id
			)
			SELECT\s
			    r.product_id,
			    COALESCE(r.recent_sum / NULLIF(m.month_sum / 4.0, 0), 1.0) AS seasonal_factor
			FROM recent r
			JOIN monthly m USING (product_id);
			""", nativeQuery = true)
	Optional<BigDecimal> seasonalFactor();
}
