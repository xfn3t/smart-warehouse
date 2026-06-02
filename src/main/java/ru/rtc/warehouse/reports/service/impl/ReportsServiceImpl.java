package ru.rtc.warehouse.reports.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.reports.dto.*;
import ru.rtc.warehouse.reports.dto.robot.RobotActivityReportDTO;
import ru.rtc.warehouse.reports.pdf.PdfReportGenerator;
import ru.rtc.warehouse.reports.repository.ReportsJdbcRepository;
import ru.rtc.warehouse.reports.service.ReportsService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportsServiceImpl implements ReportsService {

    private final ReportsJdbcRepository reportsJdbcRepository;
    private final PdfReportGenerator pdfReportGenerator;

    @Override
    public WarehouseReportDTO getFullWarehouseReport(String warehouseCode) {
        log.info("Building full report for warehouse: {}", warehouseCode);

        WarehouseReportDTO report = new WarehouseReportDTO();
        report.setReportCreatedAt(LocalDateTime.now());

        // 1. Сводка
        WarehouseSummaryReportDTO summary =
            reportsJdbcRepository.getWarehouseSummary(warehouseCode);
        report.setSummary(summary);

        // 2. Продукты
        List<ProductReportDTO> products =
            reportsJdbcRepository.getProductReport(warehouseCode);
        report.setProducts(products);

        // 3. Роботы
        List<RobotActivityReportDTO> robots =
            reportsJdbcRepository.getRobotActivityReport(warehouseCode);
        report.setRobots(robots);

        // 4. Агрегация за последние 30 дней
        List<DailyAggregationDTO> daily =
            reportsJdbcRepository.getDailyAggregationLastDays(
                warehouseCode,
                30
            );
        report.setDailyAggregation(daily);

        // 5. Расхождения
        List<DiscrepancySummaryDTO> discrepancies =
            reportsJdbcRepository.getDiscrepancySummary(warehouseCode);
        report.setDiscrepancies(discrepancies);

        // 6. История инвентаризации и расхождений по всем продуктам
        if (products != null && !products.isEmpty()) {
            List<String> allSkus = products
                .stream()
                .map(ProductReportDTO::getSkuCode)
                .collect(Collectors.toList());

            Map<String, List<ProductDailyHistoryDTO>> history =
                reportsJdbcRepository.getProductDailyHistoryForSkus(
                    warehouseCode,
                    allSkus,
                    LocalDate.now().minusDays(30),
                    LocalDate.now(),
                    4
                );
            report.setProductHistory(history);
        }

        log.info(
            "Full report for warehouse {} built: {} products, {} robots, {} daily records, {} discrepancies",
            warehouseCode,
            products != null ? products.size() : 0,
            robots != null ? robots.size() : 0,
            daily != null ? daily.size() : 0,
            discrepancies != null ? discrepancies.size() : 0
        );

        return report;
    }

    @Override
    public List<WarehouseSummaryReportDTO> getAllWarehouseSummaries() {
        log.info("Getting summaries for all warehouses");
        return reportsJdbcRepository.getAllWarehouseSummaries();
    }

    @Override
    public WarehouseSummaryReportDTO getWarehouseSummary(String warehouseCode) {
        log.info("Getting summary for warehouse: {}", warehouseCode);
        return reportsJdbcRepository.getWarehouseSummary(warehouseCode);
    }

    @Override
    public List<RobotActivityReportDTO> getRobotActivityReport(
        String warehouseCode
    ) {
        log.info(
            "Getting robot activity report for warehouse: {}",
            warehouseCode
        );
        return reportsJdbcRepository.getRobotActivityReport(warehouseCode);
    }

    @Override
    public List<RobotActivityReportDTO> getAllRobotActivityReports() {
        log.info("Getting robot activity reports for all warehouses");
        return reportsJdbcRepository.getAllRobotActivityReports();
    }

    @Override
    public List<ProductReportDTO> getProductReport(String warehouseCode) {
        log.info("Getting product report for warehouse: {}", warehouseCode);
        return reportsJdbcRepository.getProductReport(warehouseCode);
    }

    @Override
    public List<ProductReportDTO> getProductReportByCategory(
        String warehouseCode,
        String category
    ) {
        log.info(
            "Getting product report for warehouse: {} and category: {}",
            warehouseCode,
            category
        );
        return reportsJdbcRepository.getProductReportByCategory(
            warehouseCode,
            category
        );
    }

    @Override
    public List<ProductReportDTO> getLowStockProducts(String warehouseCode) {
        log.info("Getting low stock products for warehouse: {}", warehouseCode);
        return reportsJdbcRepository.getLowStockProducts(warehouseCode);
    }

    @Override
    public List<DailyAggregationDTO> getDailyAggregation(
        String warehouseCode,
        LocalDate from,
        LocalDate to
    ) {
        log.info(
            "Getting daily aggregation for warehouse: {} from {} to {}",
            warehouseCode,
            from,
            to
        );
        return reportsJdbcRepository.getDailyAggregation(
            warehouseCode,
            from,
            to
        );
    }

    @Override
    public List<DailyAggregationDTO> getDailyAggregationLastDays(
        String warehouseCode,
        int days
    ) {
        log.info(
            "Getting daily aggregation for warehouse: {} for last {} days",
            warehouseCode,
            days
        );
        return reportsJdbcRepository.getDailyAggregationLastDays(
            warehouseCode,
            days
        );
    }

    @Override
    public List<DiscrepancySummaryDTO> getDiscrepancySummary(
        String warehouseCode
    ) {
        log.info(
            "Getting discrepancy summary for warehouse: {}",
            warehouseCode
        );
        return reportsJdbcRepository.getDiscrepancySummary(warehouseCode);
    }

    @Override
    public List<DiscrepancySummaryDTO> getAllDiscrepancySummaries() {
        log.info("Getting discrepancy summaries for all warehouses");
        return reportsJdbcRepository.getAllDiscrepancySummaries();
    }

    @Override
    public byte[] generatePdfReport(String warehouseCode) {
        log.info("Generating PDF report for warehouse: {}", warehouseCode);
        WarehouseReportDTO report = getFullWarehouseReport(warehouseCode);
        return pdfReportGenerator.generateFullWarehouseReport(report);
    }

    @Override
    public byte[] generatePdfReportForSkus(
        String warehouseCode,
        List<String> skuCodes
    ) {
        log.info(
            "Generating PDF report for warehouse {} with {} SKUs",
            warehouseCode,
            skuCodes.size()
        );

        WarehouseReportDTO report = new WarehouseReportDTO();
        report.setReportCreatedAt(LocalDateTime.now());
        report.setSummary(
            reportsJdbcRepository.getWarehouseSummary(warehouseCode)
        );

        Map<String, List<ProductDailyHistoryDTO>> history =
            reportsJdbcRepository.getProductDailyHistoryForSkus(
                warehouseCode,
                skuCodes,
                LocalDate.now().minusDays(30),
                LocalDate.now(),
                4
            );
        report.setProductHistory(history);

        return pdfReportGenerator.generateSkusOnlyReport(report);
    }
}
