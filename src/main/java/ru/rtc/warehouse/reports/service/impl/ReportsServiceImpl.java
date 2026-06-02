package ru.rtc.warehouse.reports.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.rtc.warehouse.auth.UserDetailsImpl;
import ru.rtc.warehouse.reports.dto.*;
import ru.rtc.warehouse.reports.dto.robot.RobotActivityReportDTO;
import ru.rtc.warehouse.reports.model.ReportMetadata;
import ru.rtc.warehouse.reports.pdf.PdfReportGenerator;
import ru.rtc.warehouse.reports.repository.ReportMetadataRepository;
import ru.rtc.warehouse.reports.repository.ReportsJdbcRepository;
import ru.rtc.warehouse.reports.service.ReportsS3Service;
import ru.rtc.warehouse.reports.service.ReportsService;
import ru.rtc.warehouse.user.model.User;
import ru.rtc.warehouse.user.repository.UserRepository;
import ru.rtc.warehouse.warehouse.model.Warehouse;
import ru.rtc.warehouse.warehouse.service.WarehouseEntityService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportsServiceImpl implements ReportsService {

    private final ReportsJdbcRepository reportsJdbcRepository;
    private final PdfReportGenerator pdfReportGenerator;
    private final ReportsS3Service reportsS3Service;
    private final ReportMetadataRepository reportMetadataRepository;
    private final WarehouseEntityService warehouseEntityService;
    private final UserRepository userRepository;

    @Override
    public WarehouseReportDTO getFullWarehouseReport(String warehouseCode) {
        log.info("Building full report for warehouse: {}", warehouseCode);
        WarehouseReportDTO report = new WarehouseReportDTO();
        report.setReportCreatedAt(LocalDateTime.now());
        report.setSummary(
            reportsJdbcRepository.getWarehouseSummary(warehouseCode)
        );

        List<ProductReportDTO> products =
            reportsJdbcRepository.getProductReport(warehouseCode);
        report.setProducts(products);

        List<RobotActivityReportDTO> robots =
            reportsJdbcRepository.getRobotActivityReport(warehouseCode);
        report.setRobots(robots);

        List<DailyAggregationDTO> daily =
            reportsJdbcRepository.getDailyAggregationLastDays(
                warehouseCode,
                30
            );
        report.setDailyAggregation(daily);

        List<DiscrepancySummaryDTO> discrepancies =
            reportsJdbcRepository.getDiscrepancySummary(warehouseCode);
        report.setDiscrepancies(discrepancies);

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
            "Full report for warehouse {} built: {} products, {} robots, {} daily, {} discrepancies",
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
        return reportsJdbcRepository.getAllWarehouseSummaries();
    }

    @Override
    public WarehouseSummaryReportDTO getWarehouseSummary(String warehouseCode) {
        return reportsJdbcRepository.getWarehouseSummary(warehouseCode);
    }

    @Override
    public List<RobotActivityReportDTO> getRobotActivityReport(
        String warehouseCode
    ) {
        return reportsJdbcRepository.getRobotActivityReport(warehouseCode);
    }

    @Override
    public List<RobotActivityReportDTO> getAllRobotActivityReports() {
        return reportsJdbcRepository.getAllRobotActivityReports();
    }

    @Override
    public List<ProductReportDTO> getProductReport(String warehouseCode) {
        return reportsJdbcRepository.getProductReport(warehouseCode);
    }

    @Override
    public List<ProductReportDTO> getProductReportByCategory(
        String warehouseCode,
        String category
    ) {
        return reportsJdbcRepository.getProductReportByCategory(
            warehouseCode,
            category
        );
    }

    @Override
    public List<ProductReportDTO> getLowStockProducts(String warehouseCode) {
        return reportsJdbcRepository.getLowStockProducts(warehouseCode);
    }

    @Override
    public List<DailyAggregationDTO> getDailyAggregation(
        String warehouseCode,
        LocalDate from,
        LocalDate to
    ) {
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
        return reportsJdbcRepository.getDailyAggregationLastDays(
            warehouseCode,
            days
        );
    }

    @Override
    public List<DiscrepancySummaryDTO> getDiscrepancySummary(
        String warehouseCode
    ) {
        return reportsJdbcRepository.getDiscrepancySummary(warehouseCode);
    }

    @Override
    public List<DiscrepancySummaryDTO> getAllDiscrepancySummaries() {
        return reportsJdbcRepository.getAllDiscrepancySummaries();
    }

    @Override
    @Transactional
    public ReportResponseDTO generatePdfReport(
        String warehouseCode,
        UserDetailsImpl principal
    ) {
        Long userId = principal.getUser().getId();
        log.info(
            "Generating PDF report for warehouse {} by user {}",
            warehouseCode,
            userId
        );
        WarehouseReportDTO report = getFullWarehouseReport(warehouseCode);
        byte[] pdf = pdfReportGenerator.generateFullWarehouseReport(report);
        String s3Key = reportsS3Service.uploadPdf(pdf);
        ReportMetadata meta = saveMetadata(
            userId,
            warehouseCode,
            s3Key,
            "FULL_WAREHOUSE",
            null
        );
        return toResponseDto(meta, pdf);
    }

    @Override
    @Transactional
    public ReportResponseDTO generatePdfReportForSkus(
        String warehouseCode,
        List<String> skuCodes,
        UserDetailsImpl principal
    ) {
        Long userId = principal.getUser().getId();
        log.info(
            "Generating PDF report for warehouse {} with {} SKUs by user {}",
            warehouseCode,
            skuCodes.size(),
            userId
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

        byte[] pdf = pdfReportGenerator.generateSkusOnlyReport(report);
        String s3Key = reportsS3Service.uploadPdf(pdf);
        ReportMetadata meta = saveMetadata(
            userId,
            warehouseCode,
            s3Key,
            "BY_SKUS",
            skuCodes
        );
        return toResponseDto(meta, pdf);
    }

    @Override
    public List<ReportMetadataDTO> getUserReports(Long userId) {
        log.info("Getting reports for user: {}", userId);
        return reportMetadataRepository
            .findAllByUserId(userId)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<ReportMetadataDTO> getReportsByWarehouse(
        Long userId,
        String warehouseCode
    ) {
        log.info(
            "Getting reports for user {} and warehouse {}",
            userId,
            warehouseCode
        );
        return reportMetadataRepository
            .findAllByUserIdAndWarehouseCode(userId, warehouseCode)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public byte[] downloadReportFromS3(UUID reportUid) {
        ReportMetadata meta = reportMetadataRepository
            .findByReportUidAndIsDeletedFalse(reportUid)
            .orElseThrow(() ->
                new RuntimeException("Report not found: " + reportUid)
            );
        return reportsS3Service.downloadPdf(meta.getS3Key()).getByteArray();
    }

    private ReportMetadata saveMetadata(
        Long userId,
        String warehouseCode,
        String s3Key,
        String reportType,
        List<String> skuCodes
    ) {
        User user = userRepository
            .findById(userId)
            .orElseThrow(() ->
                new RuntimeException("User not found: " + userId)
            );
        Warehouse warehouse = warehouseEntityService.findByCode(warehouseCode);

        ReportMetadata meta = new ReportMetadata();
        meta.setReportUid(UUID.randomUUID());
        meta.setUser(user);
        meta.setWarehouse(warehouse);
        meta.setS3Key(s3Key);
        meta.setReportType(reportType);
        meta.setSkuCodes(skuCodes);
        meta.setCreatedAt(LocalDateTime.now());
        meta.setDeleted(false);

        reportMetadataRepository.save(meta);
        log.info("Saved report metadata: {}", meta.getReportUid());
        return meta;
    }

    private ReportResponseDTO toResponseDto(ReportMetadata meta, byte[] pdf) {
        ReportResponseDTO dto = new ReportResponseDTO();
        dto.setReportUid(meta.getReportUid());
        dto.setWarehouseCode(meta.getWarehouse().getCode());
        dto.setWarehouseName(meta.getWarehouse().getName());
        dto.setReportType(meta.getReportType());
        dto.setSkuCodes(meta.getSkuCodes());
        dto.setCreatedAt(meta.getCreatedAt());
        dto.setDownloadUrl("/api/reports/download/" + meta.getReportUid());
        dto.setPdfBase64(Base64.getEncoder().encodeToString(pdf));
        return dto;
    }

    private ReportMetadataDTO toDto(ReportMetadata meta) {
        ReportMetadataDTO dto = new ReportMetadataDTO();
        dto.setReportUid(meta.getReportUid());
        dto.setWarehouseCode(meta.getWarehouse().getCode());
        dto.setWarehouseName(meta.getWarehouse().getName());
        dto.setReportType(meta.getReportType());
        dto.setSkuCodes(meta.getSkuCodes());
        dto.setCreatedAt(meta.getCreatedAt());
        dto.setDownloadUrl("/api/reports/download/" + meta.getReportUid());
        return dto;
    }
}
