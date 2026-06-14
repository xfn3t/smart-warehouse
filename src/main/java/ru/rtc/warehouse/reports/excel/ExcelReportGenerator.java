package ru.rtc.warehouse.reports.excel;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.reports.dto.ProductReportDTO;
import ru.rtc.warehouse.reports.dto.WarehouseReportDTO;

@Slf4j
@Component
public class ExcelReportGenerator {

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] generateFullWarehouseReport(WarehouseReportDTO report) {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle dataStyle = createDataStyle(wb);

            createSummarySheet(wb, headerStyle, dataStyle, report);
            createProductsSheet(
                wb,
                headerStyle,
                dataStyle,
                report.getProducts()
            );

            return toBytes(wb);
        } catch (Exception e) {
            log.error("Ошибка генерации Excel отчёта", e);
            throw new RuntimeException(
                "Ошибка генерации Excel отчёта: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Листы: Summary, SKU Data (список ProductReportDTO).
     */
    public byte[] generateSkusOnlyReport(
        WarehouseReportDTO report,
        List<ProductReportDTO> products
    ) {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle dataStyle = createDataStyle(wb);

            createSummarySheet(wb, headerStyle, dataStyle, report);
            createProductsSheet(wb, headerStyle, dataStyle, products);

            return toBytes(wb);
        } catch (Exception e) {
            log.error("Ошибка генерации Excel отчёта по SKU", e);
            throw new RuntimeException(
                "Ошибка генерации Excel отчёта по SKU: " + e.getMessage(),
                e
            );
        }
    }

    private void createSummarySheet(
        Workbook wb,
        CellStyle headerStyle,
        CellStyle dataStyle,
        WarehouseReportDTO report
    ) {
        Sheet sheet = wb.createSheet("Summary");
        Row header = sheet.createRow(0);
        String[] cols = {
            "Warehouse",
            "Total Qty",
            "Unique SKUs",
            "Total Discrepancies",
            "Critical",
            "Low Stock",
            "Last Scan",
        };
        for (int i = 0; i < cols.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(headerStyle);
        }

        Row row = sheet.createRow(1);
        var summary = report.getSummary();
        if (summary != null) {
            fillCell(
                row,
                0,
                summary.getWarehouseCode() + " - " + summary.getWarehouseName(),
                dataStyle
            );
            fillCell(row, 1, nvl(summary.getTotalQuantity()), dataStyle);
            fillCell(row, 2, nvl(summary.getUniqueSkuCount()), dataStyle);
            fillCell(row, 3, nvl(summary.getTotalDiscrepancy()), dataStyle);
            fillCell(row, 4, nvl(summary.getCriticalCount()), dataStyle);
            fillCell(row, 5, nvl(summary.getLowStockCount()), dataStyle);
            fillCell(
                row,
                6,
                summary.getLastScanAt() != null
                    ? summary.getLastScanAt().format(DATE_FMT)
                    : "",
                dataStyle
            );
        }

        for (int i = 0; i < cols.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createProductsSheet(
        Workbook wb,
        CellStyle headerStyle,
        CellStyle dataStyle,
        List<ProductReportDTO> products
    ) {
        Sheet sheet = wb.createSheet("Products");
        Row header = sheet.createRow(0);
        String[] cols = {
            "SKU Code",
            "Product Name",
            "Category",
            "Min Stock",
            "Optimal Stock",
            "Expected Qty",
            "Current Qty",
            "Difference",
            "Status",
            "Last Scanned",
        };
        for (int i = 0; i < cols.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        if (products != null) {
            for (ProductReportDTO p : products) {
                Row row = sheet.createRow(rowIdx++);
                fillCell(row, 0, p.getSkuCode(), dataStyle);
                fillCell(row, 1, p.getProductName(), dataStyle);
                fillCell(row, 2, p.getCategory(), dataStyle);
                fillCell(row, 3, nvl(p.getMinStock()), dataStyle);
                fillCell(row, 4, nvl(p.getOptimalStock()), dataStyle);
                fillCell(row, 5, nvl(p.getExpectedQuantity()), dataStyle);
                fillCell(row, 6, nvl(p.getCurrentQuantity()), dataStyle);
                fillCell(row, 7, nvl(p.getDifference()), dataStyle);
                fillCell(row, 8, p.getInventoryStatus(), dataStyle);
                fillCell(
                    row,
                    9,
                    p.getLastScannedAt() != null
                        ? p.getLastScannedAt().format(DATE_FMT)
                        : "",
                    dataStyle
                );
            }
        }

        for (int i = 0; i < cols.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void fillCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private String nvl(Object val) {
        return val != null ? String.valueOf(val) : "";
    }

    private byte[] toBytes(Workbook wb) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка записи Excel в байты", e);
        }
    }
}
