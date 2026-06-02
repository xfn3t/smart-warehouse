package ru.rtc.warehouse.reports.service;

import java.time.LocalDate;
import java.util.List;
import ru.rtc.warehouse.reports.dto.*;
import ru.rtc.warehouse.reports.dto.robot.RobotActivityReportDTO;

public interface ReportsService {
    /**
     * Полный отчет по складу: сводка + продукты + роботы + агрегация + расхождения
     */
    WarehouseReportDTO getFullWarehouseReport(String warehouseCode);

    /**
     * Сводный отчет по всем складам
     */
    List<WarehouseSummaryReportDTO> getAllWarehouseSummaries();

    /**
     * Сводка по конкретному складу
     */
    WarehouseSummaryReportDTO getWarehouseSummary(String warehouseCode);

    /**
     * Отчет по роботам на складе
     */
    List<RobotActivityReportDTO> getRobotActivityReport(String warehouseCode);

    /**
     * Отчет по роботам по всем складам
     */
    List<RobotActivityReportDTO> getAllRobotActivityReports();

    /**
     * Отчет по продуктам на складе
     */
    List<ProductReportDTO> getProductReport(String warehouseCode);

    /**
     * Отчет по продуктам по категории
     */
    List<ProductReportDTO> getProductReportByCategory(
        String warehouseCode,
        String category
    );

    /**
     * Продукты с низким запасом / критическим статусом
     */
    List<ProductReportDTO> getLowStockProducts(String warehouseCode);

    /**
     * Ежедневная агрегация за период
     */
    List<DailyAggregationDTO> getDailyAggregation(
        String warehouseCode,
        LocalDate from,
        LocalDate to
    );

    /**
     * Ежедневная агрегация за последние N дней
     */
    List<DailyAggregationDTO> getDailyAggregationLastDays(
        String warehouseCode,
        int days
    );

    /**
     * Сводка расхождений за последние 30 дней
     */
    List<DiscrepancySummaryDTO> getDiscrepancySummary(String warehouseCode);

    List<DiscrepancySummaryDTO> getAllDiscrepancySummaries();

    byte[] generatePdfReport(String warehouseCode);

    byte[] generatePdfReportForSkus(
        String warehouseCode,
        List<String> skuCodes
    );
}
