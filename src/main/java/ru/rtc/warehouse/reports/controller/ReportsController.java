package ru.rtc.warehouse.reports.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.rtc.warehouse.auth.UserDetailsImpl;
import ru.rtc.warehouse.common.aspect.RequiresOwnership;
import ru.rtc.warehouse.reports.dto.*;
import ru.rtc.warehouse.reports.dto.robot.RobotActivityReportDTO;
import ru.rtc.warehouse.reports.service.ReportsService;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportsController {

    private final ReportsService reportsService;

    @GetMapping("/warehouses/{warehouseCode}/full")
    @RequiresOwnership(
        codeParam = "warehouseCode",
        entityType = RequiresOwnership.EntityType.WAREHOUSE
    )
    public ResponseEntity<WarehouseReportDTO> getFullWarehouseReport(
        @PathVariable String warehouseCode
    ) {
        return ResponseEntity.ok(
            reportsService.getFullWarehouseReport(warehouseCode)
        );
    }

    @GetMapping("/warehouses/summaries")
    public ResponseEntity<
        List<WarehouseSummaryReportDTO>
    > getAllWarehouseSummaries() {
        return ResponseEntity.ok(reportsService.getAllWarehouseSummaries());
    }

    @GetMapping("/warehouses/{warehouseCode}/summary")
    @RequiresOwnership(
        codeParam = "warehouseCode",
        entityType = RequiresOwnership.EntityType.WAREHOUSE
    )
    public ResponseEntity<WarehouseSummaryReportDTO> getWarehouseSummary(
        @PathVariable String warehouseCode
    ) {
        return ResponseEntity.ok(
            reportsService.getWarehouseSummary(warehouseCode)
        );
    }

    @GetMapping("/robots")
    public ResponseEntity<
        List<RobotActivityReportDTO>
    > getAllRobotActivityReports() {
        return ResponseEntity.ok(reportsService.getAllRobotActivityReports());
    }

    @GetMapping("/warehouses/{warehouseCode}/robots")
    @RequiresOwnership(
        codeParam = "warehouseCode",
        entityType = RequiresOwnership.EntityType.WAREHOUSE
    )
    public ResponseEntity<List<RobotActivityReportDTO>> getRobotActivityReport(
        @PathVariable String warehouseCode
    ) {
        return ResponseEntity.ok(
            reportsService.getRobotActivityReport(warehouseCode)
        );
    }

    @GetMapping("/warehouses/{warehouseCode}/products")
    @RequiresOwnership(
        codeParam = "warehouseCode",
        entityType = RequiresOwnership.EntityType.WAREHOUSE
    )
    public ResponseEntity<List<ProductReportDTO>> getProductReport(
        @PathVariable String warehouseCode
    ) {
        return ResponseEntity.ok(
            reportsService.getProductReport(warehouseCode)
        );
    }

    @GetMapping("/warehouses/{warehouseCode}/products/by-category")
    @RequiresOwnership(
        codeParam = "warehouseCode",
        entityType = RequiresOwnership.EntityType.WAREHOUSE
    )
    public ResponseEntity<List<ProductReportDTO>> getProductReportByCategory(
        @PathVariable String warehouseCode,
        @RequestParam String category
    ) {
        return ResponseEntity.ok(
            reportsService.getProductReportByCategory(warehouseCode, category)
        );
    }

    @GetMapping("/warehouses/{warehouseCode}/products/low-stock")
    @RequiresOwnership(
        codeParam = "warehouseCode",
        entityType = RequiresOwnership.EntityType.WAREHOUSE
    )
    public ResponseEntity<List<ProductReportDTO>> getLowStockProducts(
        @PathVariable String warehouseCode
    ) {
        return ResponseEntity.ok(
            reportsService.getLowStockProducts(warehouseCode)
        );
    }

    @GetMapping("/warehouses/{warehouseCode}/daily")
    @RequiresOwnership(
        codeParam = "warehouseCode",
        entityType = RequiresOwnership.EntityType.WAREHOUSE
    )
    public ResponseEntity<List<DailyAggregationDTO>> getDailyAggregation(
        @PathVariable String warehouseCode,
        @RequestParam @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate from,
        @RequestParam @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate to
    ) {
        return ResponseEntity.ok(
            reportsService.getDailyAggregation(warehouseCode, from, to)
        );
    }

    @GetMapping("/warehouses/{warehouseCode}/daily/last")
    @RequiresOwnership(
        codeParam = "warehouseCode",
        entityType = RequiresOwnership.EntityType.WAREHOUSE
    )
    public ResponseEntity<
        List<DailyAggregationDTO>
    > getDailyAggregationLastDays(
        @PathVariable String warehouseCode,
        @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(
            reportsService.getDailyAggregationLastDays(warehouseCode, days)
        );
    }

    @GetMapping("/discrepancies")
    public ResponseEntity<
        List<DiscrepancySummaryDTO>
    > getAllDiscrepancySummaries() {
        return ResponseEntity.ok(reportsService.getAllDiscrepancySummaries());
    }

    @GetMapping("/warehouses/{warehouseCode}/discrepancies")
    @RequiresOwnership(
        codeParam = "warehouseCode",
        entityType = RequiresOwnership.EntityType.WAREHOUSE
    )
    public ResponseEntity<List<DiscrepancySummaryDTO>> getDiscrepancySummary(
        @PathVariable String warehouseCode
    ) {
        return ResponseEntity.ok(
            reportsService.getDiscrepancySummary(warehouseCode)
        );
    }

    @GetMapping("/warehouses/{warehouseCode}/pdf")
    @RequiresOwnership(
        codeParam = "warehouseCode",
        entityType = RequiresOwnership.EntityType.WAREHOUSE
    )
    public ResponseEntity<ReportResponseDTO> generatePdf(
        @PathVariable String warehouseCode,
        @AuthenticationPrincipal UserDetailsImpl principal
    ) {
        return ResponseEntity.ok(
            reportsService.generatePdfReport(warehouseCode, principal)
        );
    }

    @PostMapping("/warehouses/{warehouseCode}/pdf/by-skus")
    @RequiresOwnership(
        codeParam = "warehouseCode",
        entityType = RequiresOwnership.EntityType.WAREHOUSE
    )
    public ResponseEntity<ReportResponseDTO> generatePdfForSkus(
        @PathVariable String warehouseCode,
        @RequestBody List<String> skuCodes,
        @AuthenticationPrincipal UserDetailsImpl principal
    ) {
        return ResponseEntity.ok(
            reportsService.generatePdfReportForSkus(
                warehouseCode,
                skuCodes,
                principal
            )
        );
    }

    @GetMapping("/user")
    public ResponseEntity<List<ReportMetadataDTO>> getUserReports(
        @AuthenticationPrincipal UserDetailsImpl principal
    ) {
        return ResponseEntity.ok(
            reportsService.getUserReports(principal.getUser().getId())
        );
    }

    @GetMapping("/warehouses/{warehouseCode}/reports")
    @RequiresOwnership(
        codeParam = "warehouseCode",
        entityType = RequiresOwnership.EntityType.WAREHOUSE
    )
    public ResponseEntity<List<ReportMetadataDTO>> getWarehouseReports(
        @PathVariable String warehouseCode,
        @AuthenticationPrincipal UserDetailsImpl principal
    ) {
        return ResponseEntity.ok(
            reportsService.getReportsByWarehouse(
                principal.getUser().getId(),
                warehouseCode
            )
        );
    }

    @GetMapping("/download/{reportUid}")
    public ResponseEntity<byte[]> downloadFromS3(@PathVariable UUID reportUid) {
        byte[] pdf = reportsService.downloadReportFromS3(reportUid);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"report.pdf\""
            )
            .body(pdf);
    }
}
