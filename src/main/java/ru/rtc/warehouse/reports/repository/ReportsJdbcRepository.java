package ru.rtc.warehouse.reports.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.rtc.warehouse.reports.dto.*;
import ru.rtc.warehouse.reports.dto.robot.RobotActivityReportDTO;
import ru.rtc.warehouse.reports.mapper.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ReportsJdbcRepository {

    private final JdbcTemplate jdbcTemplate;
    private final WarehouseSummaryRowMapper warehouseSummaryRowMapper;
    private final RobotActivityRowMapper robotActivityRowMapper;
    private final ProductReportRowMapper productReportRowMapper;
    private final DailyAggregationRowMapper dailyAggregationRowMapper;
    private final DiscrepancySummaryRowMapper discrepancySummaryRowMapper;
    private final ProductDailyHistoryRowMapper productDailyHistoryRowMapper;

    public WarehouseSummaryReportDTO getWarehouseSummary(String warehouseCode) {
        String sql = """
            SELECT
                warehouse_id,
                warehouse_code,
                warehouse_name,
                total_quantity,
                unique_sku_count,
                total_discrepancy,
                total_abs_discrepancy,
                last_scan_at,
                critical_count,
                low_stock_count
            FROM v_warehouse_summary
            WHERE warehouse_code = ?
            """;
        return jdbcTemplate.queryForObject(
            sql,
            warehouseSummaryRowMapper,
            warehouseCode
        );
    }

    public List<WarehouseSummaryReportDTO> getAllWarehouseSummaries() {
        String sql = """
            SELECT
                warehouse_id,
                warehouse_code,
                warehouse_name,
                total_quantity,
                unique_sku_count,
                total_discrepancy,
                total_abs_discrepancy,
                last_scan_at,
                critical_count,
                low_stock_count
            FROM v_warehouse_summary
            ORDER BY warehouse_code
            """;
        return jdbcTemplate.query(sql, warehouseSummaryRowMapper);
    }

    public List<RobotActivityReportDTO> getRobotActivityReport(
        String warehouseCode
    ) {
        String sql = """
            SELECT
                robot_id,
                robot_code,
                warehouse_code,
                warehouse_name,
                robot_status,
                battery_level,
                last_update,
                total_scans,
                last_scan_at,
                ok_scans,
                low_stock_scans,
                critical_scans,
                total_difference
            FROM v_robot_activity_report
            WHERE warehouse_code = ?
            ORDER BY total_scans DESC
            """;
        return jdbcTemplate.query(sql, robotActivityRowMapper, warehouseCode);
    }

    public List<RobotActivityReportDTO> getAllRobotActivityReports() {
        String sql = """
            SELECT
                robot_id,
                robot_code,
                warehouse_code,
                warehouse_name,
                robot_status,
                battery_level,
                last_update,
                total_scans,
                last_scan_at,
                ok_scans,
                low_stock_scans,
                critical_scans,
                total_difference
            FROM v_robot_activity_report
            ORDER BY warehouse_code, total_scans DESC
            """;
        return jdbcTemplate.query(sql, robotActivityRowMapper);
    }

    public List<ProductReportDTO> getProductReport(String warehouseCode) {
        String sql = """
            SELECT
                product_id,
                sku_code,
                product_name,
                category,
                warehouse_code,
                warehouse_name,
                min_stock,
                optimal_stock,
                current_quantity,
                expected_quantity,
                difference,
                inventory_status,
                last_scanned_at
            FROM v_product_report
            WHERE warehouse_code = ?
            ORDER BY COALESCE(difference, 0) DESC
            """;
        return jdbcTemplate.query(sql, productReportRowMapper, warehouseCode);
    }

    public ProductReportDTO getProductReportBySku(
        String warehouseCode,
        String skuCode
    ) {
        String sql = """
            SELECT
                product_id,
                sku_code,
                product_name,
                category,
                warehouse_code,
                warehouse_name,
                min_stock,
                optimal_stock,
                current_quantity,
                expected_quantity,
                difference,
                inventory_status,
                last_scanned_at
            FROM v_product_report
            WHERE warehouse_code = ? AND sku_code = ?
            """;
        List<ProductReportDTO> result = jdbcTemplate.query(
            sql,
            productReportRowMapper,
            warehouseCode,
            skuCode
        );
        return result.isEmpty() ? null : result.get(0);
    }

    public List<ProductReportDTO> getProductReportByCategory(
        String warehouseCode,
        String category
    ) {
        String sql = """
            SELECT
                product_id,
                sku_code,
                product_name,
                category,
                warehouse_code,
                warehouse_name,
                min_stock,
                optimal_stock,
                current_quantity,
                expected_quantity,
                difference,
                inventory_status,
                last_scanned_at
            FROM v_product_report
            WHERE warehouse_code = ?
              AND category = ?
            ORDER BY COALESCE(difference, 0) DESC
            """;
        return jdbcTemplate.query(
            sql,
            productReportRowMapper,
            warehouseCode,
            category
        );
    }

    public List<ProductReportDTO> getLowStockProducts(String warehouseCode) {
        String sql = """
            SELECT
                product_id,
                sku_code,
                product_name,
                category,
                warehouse_code,
                warehouse_name,
                min_stock,
                optimal_stock,
                current_quantity,
                expected_quantity,
                difference,
                inventory_status,
                last_scanned_at
            FROM v_product_report
            WHERE warehouse_code = ?
              AND (inventory_status = 'CRITICAL' OR inventory_status = 'LOW_STOCK')
            ORDER BY COALESCE(current_quantity, 0) ASC
            """;
        return jdbcTemplate.query(sql, productReportRowMapper, warehouseCode);
    }

    public List<DailyAggregationDTO> getDailyAggregation(
        String warehouseCode,
        LocalDate from,
        LocalDate to
    ) {
        String sql = """
            SELECT
                warehouse_id,
                warehouse_code,
                scan_date,
                total_scans,
                total_quantity,
                total_difference,
                total_abs_difference,
                unique_products_scanned,
                unique_robots
            FROM v_daily_aggregation
            WHERE warehouse_code = ?
              AND scan_date BETWEEN ? AND ?
            ORDER BY scan_date
            """;
        return jdbcTemplate.query(
            sql,
            dailyAggregationRowMapper,
            warehouseCode,
            from,
            to
        );
    }

    public List<DailyAggregationDTO> getDailyAggregationLastDays(
        String warehouseCode,
        int days
    ) {
        String sql = """
            SELECT
                warehouse_id,
                warehouse_code,
                scan_date,
                total_scans,
                total_quantity,
                total_difference,
                total_abs_difference,
                unique_products_scanned,
                unique_robots
            FROM v_daily_aggregation
            WHERE warehouse_code = ?
              AND scan_date >= CURRENT_DATE - ?
            ORDER BY scan_date
            """;
        return jdbcTemplate.query(
            sql,
            dailyAggregationRowMapper,
            warehouseCode,
            days
        );
    }

    public List<DiscrepancySummaryDTO> getDiscrepancySummary(
        String warehouseCode
    ) {
        String sql = """
            SELECT
                warehouse_code,
                warehouse_name,
                sku_code,
                product_name,
                scan_count,
                total_abs_discrepancy,
                avg_discrepancy,
                max_discrepancy,
                last_discrepancy_at
            FROM v_discrepancy_summary
            WHERE warehouse_code = ?
            ORDER BY total_abs_discrepancy DESC
            """;
        return jdbcTemplate.query(
            sql,
            discrepancySummaryRowMapper,
            warehouseCode
        );
    }

    public List<DiscrepancySummaryDTO> getAllDiscrepancySummaries() {
        String sql = """
            SELECT
                warehouse_code,
                warehouse_name,
                sku_code,
                product_name,
                scan_count,
                total_abs_discrepancy,
                avg_discrepancy,
                max_discrepancy,
                last_discrepancy_at
            FROM v_discrepancy_summary
            ORDER BY total_abs_discrepancy DESC
            """;
        return jdbcTemplate.query(sql, discrepancySummaryRowMapper);
    }

    public Long getTotalProductCount(String warehouseCode) {
        String sql = """
            SELECT COALESCE(SUM(latest.quantity), 0)
            FROM (
                SELECT DISTINCT ON (ih.product_id)
                    ih.quantity
                FROM inventory_history ih
                JOIN warehouses w ON w.id = ih.warehouse_id
                WHERE w.code = ?
                  AND ih.is_deleted = false
                ORDER BY ih.product_id, ih.scanned_at DESC
            ) latest
            """;
        return jdbcTemplate.queryForObject(sql, Long.class, warehouseCode);
    }

    public Long getUniqueSkuCount(String warehouseCode) {
        String sql = """
            SELECT COUNT(DISTINCT p.id)
            FROM products p
            JOIN product_warehouse pw ON pw.product_id = p.id AND pw.is_deleted = false
            JOIN warehouses w ON w.id = pw.warehouse_id AND w.is_deleted = false
            WHERE w.code = ?
              AND p.is_deleted = false
            """;
        return jdbcTemplate.queryForObject(sql, Long.class, warehouseCode);
    }

    public Long getTotalDiscrepancy(String warehouseCode) {
        String sql = """
            SELECT COALESCE(SUM(latest.difference), 0)
            FROM (
                SELECT DISTINCT ON (ih.product_id)
                    ih.difference
                FROM inventory_history ih
                JOIN warehouses w ON w.id = ih.warehouse_id
                WHERE w.code = ?
                  AND ih.is_deleted = false
                ORDER BY ih.product_id, ih.scanned_at DESC
            ) latest
            """;
        return jdbcTemplate.queryForObject(sql, Long.class, warehouseCode);
    }

    public LocalDateTime getLastScanTime(String warehouseCode) {
        String sql = """
            SELECT MAX(ih.scanned_at)
            FROM inventory_history ih
            JOIN warehouses w ON w.id = ih.warehouse_id
            WHERE w.code = ?
              AND ih.is_deleted = false
            """;
        return jdbcTemplate.queryForObject(
            sql,
            LocalDateTime.class,
            warehouseCode
        );
    }

    public Long getRobotCount(String warehouseCode) {
        String sql = """
            SELECT COUNT(*)
            FROM robots r
            JOIN warehouses w ON w.id = r.warehouse_id
            WHERE w.code = ?
              AND r.is_deleted = false
            """;
        return jdbcTemplate.queryForObject(sql, Long.class, warehouseCode);
    }

    public List<Object[]> getRobotStatusDistribution(String warehouseCode) {
        String sql = """
            SELECT rs.code, COUNT(*)
            FROM robots r
            JOIN warehouses w ON w.id = r.warehouse_id
            JOIN robot_status rs ON rs.id = r.status_id
            WHERE w.code = ?
              AND r.is_deleted = false
            GROUP BY rs.code
            ORDER BY COUNT(*) DESC
            """;
        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new Object[] {
                rs.getString("code"),
                rs.getLong("count"),
            },
            warehouseCode
        );
    }

    public List<DailyAggregationDTO> getDailyAggregationDirect(
        String warehouseCode,
        String startDate,
        String endDate
    ) {
        String sql = """
            SELECT
                w.id AS warehouse_id,
                w.code AS warehouse_code,
                DATE(ih.scanned_at) AS scan_date,
                COUNT(*) AS total_scans,
                COALESCE(SUM(ih.quantity), 0) AS total_quantity,
                COALESCE(SUM(ih.difference), 0) AS total_difference,
                COALESCE(SUM(ABS(ih.difference)), 0) AS total_abs_difference,
                COUNT(DISTINCT ih.product_id) AS unique_products_scanned,
                COUNT(DISTINCT ih.robot_id) AS unique_robots
            FROM inventory_history ih
            JOIN warehouses w ON w.id = ih.warehouse_id AND w.is_deleted = false
            WHERE w.code = ?
              AND ih.is_deleted = false
              AND ih.scanned_at::date BETWEEN ?::date AND ?::date
            GROUP BY w.id, w.code, DATE(ih.scanned_at)
            ORDER BY scan_date
            """;
        return jdbcTemplate.query(
            sql,
            dailyAggregationRowMapper,
            warehouseCode,
            startDate,
            endDate
        );
    }

    public List<Object[]> getTopDiscrepancyProducts(
        String warehouseCode,
        int limit
    ) {
        String sql = """
            SELECT
                p.sku_code,
                p.name,
                SUM(ABS(ih.difference)) AS total_abs_diff,
                COUNT(*) AS scan_count,
                MAX(ih.scanned_at) AS last_scan
            FROM inventory_history ih
            JOIN warehouses w ON w.id = ih.warehouse_id AND w.is_deleted = false
            JOIN products p ON p.id = ih.product_id AND p.is_deleted = false
            WHERE w.code = ?
              AND ih.is_deleted = false
              AND ih.difference != 0
            GROUP BY p.sku_code, p.name
            ORDER BY total_abs_diff DESC
            LIMIT ?
            """;
        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new Object[] {
                rs.getString("sku_code"),
                rs.getString("name"),
                rs.getLong("total_abs_diff"),
                rs.getLong("scan_count"),
                rs.getTimestamp("last_scan") != null
                    ? rs.getTimestamp("last_scan").toLocalDateTime()
                    : null,
            },
            warehouseCode,
            limit
        );
    }

    public List<ProductDailyHistoryDTO> getProductDailyHistory(
        String warehouseCode,
        String skuCode,
        LocalDate from,
        LocalDate to
    ) {
        String sql = """
            SELECT
                p.sku_code,
                p.name AS product_name,
                DATE(ih.scanned_at) AS scan_date,
                COALESCE(SUM(ih.quantity), 0) AS daily_quantity,
                COALESCE(SUM(ih.difference), 0) AS daily_difference
            FROM inventory_history ih
            JOIN warehouses w ON w.id = ih.warehouse_id AND w.is_deleted = false
            JOIN products p ON p.id = ih.product_id AND p.is_deleted = false
            WHERE w.code = ?
              AND p.sku_code = ?
              AND ih.is_deleted = false
              AND ih.scanned_at::date BETWEEN ?::date AND ?::date
            GROUP BY p.sku_code, p.name, DATE(ih.scanned_at)
            ORDER BY scan_date
            """;
        return jdbcTemplate.query(
            sql,
            productDailyHistoryRowMapper,
            warehouseCode,
            skuCode,
            from,
            to
        );
    }

    public Map<
        String,
        List<ProductDailyHistoryDTO>
    > getProductDailyHistoryForSkus(
        String warehouseCode,
        java.util.List<String> skuCodes,
        LocalDate from,
        LocalDate to,
        int maxConcurrency
    ) {
        Map<String, List<ProductDailyHistoryDTO>> result =
            new ConcurrentHashMap<>();
        Semaphore semaphore = new Semaphore(maxConcurrency);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        java.util.List<Future<?>> futures = new ArrayList<>();
        for (String sku : skuCodes) {
            futures.add(
                executor.submit(() -> {
                    try {
                        semaphore.acquire();
                        List<ProductDailyHistoryDTO> history =
                            getProductDailyHistory(
                                warehouseCode,
                                sku,
                                from,
                                to
                            );
                        if (!history.isEmpty()) {
                            result.put(sku, history);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        semaphore.release();
                    }
                })
            );
        }

        for (Future<?> f : futures) {
            try {
                f.get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn(
                    "Timeout or error fetching product history: {}",
                    e.getMessage()
                );
            }
        }
        executor.shutdownNow();
        return result;
    }
}
