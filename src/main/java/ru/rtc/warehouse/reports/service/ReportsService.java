package ru.rtc.warehouse.reports.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import ru.rtc.warehouse.auth.UserDetailsImpl;
import ru.rtc.warehouse.reports.dto.*;
import ru.rtc.warehouse.reports.dto.robot.RobotActivityReportDTO;

public interface ReportsService {
    WarehouseReportDTO getFullWarehouseReport(String warehouseCode);

    List<WarehouseSummaryReportDTO> getAllWarehouseSummaries();

    WarehouseSummaryReportDTO getWarehouseSummary(String warehouseCode);

    List<RobotActivityReportDTO> getRobotActivityReport(String warehouseCode);

    List<RobotActivityReportDTO> getAllRobotActivityReports();

    List<ProductReportDTO> getProductReport(String warehouseCode);

    List<ProductReportDTO> getProductReportByCategory(
        String warehouseCode,
        String category
    );

    List<ProductReportDTO> getLowStockProducts(String warehouseCode);

    List<DailyAggregationDTO> getDailyAggregation(
        String warehouseCode,
        LocalDate from,
        LocalDate to
    );

    List<DailyAggregationDTO> getDailyAggregationLastDays(
        String warehouseCode,
        int days
    );

    List<DiscrepancySummaryDTO> getDiscrepancySummary(String warehouseCode);

    List<DiscrepancySummaryDTO> getAllDiscrepancySummaries();

    ReportResponseDTO generatePdfReport(
        String warehouseCode,
        UserDetailsImpl principal
    );

    ReportResponseDTO generatePdfReportForSkus(
        String warehouseCode,
        List<String> skuCodes,
        UserDetailsImpl principal
    );

    ReportResponseDTO generateExcelReport(
        String warehouseCode,
        UserDetailsImpl principal
    );

    ReportResponseDTO generateExcelReportForSkus(
        String warehouseCode,
        List<String> skuCodes,
        UserDetailsImpl principal
    );

    List<ReportMetadataDTO> getUserReports(Long userId);

    List<ReportMetadataDTO> getReportsByWarehouse(
        Long userId,
        String warehouseCode
    );

    ReportFileDTO downloadReportFromS3(UUID reportUid);
}
