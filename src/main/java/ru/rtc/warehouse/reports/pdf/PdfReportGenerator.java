package ru.rtc.warehouse.reports.pdf;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.rtc.warehouse.reports.dto.*;
import ru.rtc.warehouse.reports.dto.robot.RobotActivityReportDTO;

@Slf4j
@Component
@RequiredArgsConstructor
public class PdfReportGenerator {

    private final PdfFontProvider fonts;
    private final PdfChartRenderer chartRenderer;

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final float MARGIN = 36f;

    public byte[] generateFullWarehouseReport(WarehouseReportDTO report) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(
            PageSize.A4,
            MARGIN,
            MARGIN,
            MARGIN,
            MARGIN
        );
        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();
            addTitle(doc, report);
            addSummaryTable(doc, report.getSummary());

            if (report.getRobots() != null && !report.getRobots().isEmpty()) {
                addRobotStatusChart(doc, report.getRobots());
                addRobotTable(doc, report.getRobots());
            }
            if (
                report.getProducts() != null && !report.getProducts().isEmpty()
            ) {
                addProductTable(doc, report.getProducts());
            }
            if (
                report.getDailyAggregation() != null &&
                !report.getDailyAggregation().isEmpty()
            ) {
                addDailyChart(doc, report.getDailyAggregation());
            }
            if (
                report.getDiscrepancies() != null &&
                !report.getDiscrepancies().isEmpty()
            ) {
                addDiscrepancyChart(doc, report.getDiscrepancies());
                addDiscrepancyTable(doc, report.getDiscrepancies());
            }
            if (
                report.getProductHistory() != null &&
                !report.getProductHistory().isEmpty()
            ) {
                addProductHistoryCharts(doc, report.getProductHistory());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        } finally {
            doc.close();
        }
        return baos.toByteArray();
    }

    public byte[] generateSkusOnlyReport(WarehouseReportDTO report) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(
            PageSize.A4,
            MARGIN,
            MARGIN,
            MARGIN,
            MARGIN
        );
        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();
            addTitle(doc, report);
            addSummaryTable(doc, report.getSummary());

            if (
                report.getProductHistory() != null &&
                !report.getProductHistory().isEmpty()
            ) {
                doc.newPage();
                addProductHistoryCharts(doc, report.getProductHistory());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        } finally {
            doc.close();
        }
        return baos.toByteArray();
    }

    private void addTitle(Document doc, WarehouseReportDTO report)
        throws DocumentException {
        Paragraph title = new Paragraph("ОТЧЁТ ПО СКЛАДУ", fonts.bold(18));
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);

        Paragraph meta = new Paragraph(
            String.format(
                "Склад: %s | Сформирован: %s",
                report.getSummary() != null
                    ? report.getSummary().getWarehouseName()
                    : "-",
                report.getReportCreatedAt() != null
                    ? report.getReportCreatedAt().format(DATE_FMT)
                    : LocalDateTime.now().format(DATE_FMT)
            ),
            fonts.regular(10)
        );
        meta.setAlignment(Element.ALIGN_CENTER);
        doc.add(meta);
        doc.add(new Paragraph(" ", fonts.regular(6)));
    }

    private void addSummaryTable(
        Document doc,
        WarehouseSummaryReportDTO summary
    ) throws DocumentException {
        if (summary == null) return;
        doc.add(new Paragraph("Сводка", fonts.bold(14)));
        doc.add(new Paragraph(" ", fonts.regular(4)));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 50, 50 });

        addKeyValue(table, "Код склада", summary.getWarehouseCode());
        addKeyValue(table, "Название", summary.getWarehouseName());
        addKeyValue(
            table,
            "Всего товаров (шт.)",
            String.valueOf(summary.getTotalQuantity())
        );
        addKeyValue(
            table,
            "Уникальных SKU",
            String.valueOf(summary.getUniqueSkuCount())
        );
        addKeyValue(
            table,
            "Суммарное расхождение",
            String.valueOf(summary.getTotalDiscrepancy())
        );
        addKeyValue(
            table,
            "Суммарное |расхождение|",
            String.valueOf(summary.getTotalAbsDiscrepancy())
        );
        addKeyValue(
            table,
            "Критических позиций",
            String.valueOf(summary.getCriticalCount())
        );
        addKeyValue(
            table,
            "Низкий запас (позиций)",
            String.valueOf(summary.getLowStockCount())
        );
        addKeyValue(
            table,
            "Последнее сканирование",
            summary.getLastScanAt() != null
                ? summary.getLastScanAt().format(DATE_FMT)
                : "-"
        );
        doc.add(table);
        doc.add(new Paragraph(" ", fonts.regular(8)));
    }

    private void addRobotStatusChart(
        Document doc,
        List<RobotActivityReportDTO> robots
    ) throws DocumentException {
        Map<String, Integer> statusDistribution = robots
            .stream()
            .collect(
                Collectors.groupingBy(
                    RobotActivityReportDTO::getRobotStatus,
                    Collectors.summingInt(r -> 1)
                )
            );
        Image chart = chartRenderer.renderRobotStatusPieChart(
            statusDistribution,
            "Статусы роботов"
        );
        chart.scalePercent(70);
        chart.setAlignment(Image.ALIGN_CENTER);
        doc.add(chart);
        doc.add(new Paragraph(" ", fonts.regular(4)));
    }

    private void addRobotTable(
        Document doc,
        List<RobotActivityReportDTO> robots
    ) throws DocumentException {
        doc.add(new Paragraph("Активность роботов", fonts.bold(14)));
        doc.add(new Paragraph(" ", fonts.regular(4)));

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 18, 14, 14, 14, 14, 14, 12 });

        String[] headers = {
            "Робот",
            "Статус",
            "Батарея",
            "Сканов",
            "OK",
            "LOW",
            "CRIT",
        };
        for (String h : headers) {
            table.addCell(headerCell(h));
        }
        for (RobotActivityReportDTO r : robots) {
            table.addCell(cell(r.getRobotCode(), 8));
            table.addCell(cell(r.getRobotStatus(), 8));
            table.addCell(cell(r.getBatteryLevel() + "%", 8));
            table.addCell(cell(String.valueOf(r.getTotalScans()), 8));
            table.addCell(cell(String.valueOf(r.getOkScans()), 8));
            table.addCell(cell(String.valueOf(r.getLowStockScans()), 8));
            table.addCell(cell(String.valueOf(r.getCriticalScans()), 8));
        }
        doc.add(table);
        doc.add(new Paragraph(" ", fonts.regular(8)));
    }

    private void addProductTable(Document doc, List<ProductReportDTO> products)
        throws DocumentException {
        doc.add(new Paragraph("Продукты", fonts.bold(14)));
        doc.add(new Paragraph(" ", fonts.regular(4)));

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 18, 16, 12, 12, 12, 14, 16 });

        String[] headers = {
            "SKU",
            "Название",
            "Текущее",
            "Ожид.",
            "Мин.",
            "Расхожд.",
            "Статус",
        };
        for (String h : headers) {
            table.addCell(headerCell(h));
        }
        int limit = Math.min(products.size(), 40);
        for (int i = 0; i < limit; i++) {
            ProductReportDTO p = products.get(i);
            table.addCell(cell(p.getSkuCode(), 8));
            table.addCell(cell(ellipsis(p.getProductName(), 20), 8));
            table.addCell(cell(nvl(p.getCurrentQuantity()), 8));
            table.addCell(cell(nvl(p.getExpectedQuantity()), 8));
            table.addCell(cell(String.valueOf(p.getMinStock()), 8));
            table.addCell(cell(nvl(p.getDifference()), 8));
            table.addCell(cell(nvl(p.getInventoryStatus()), 8));
        }
        if (products.size() > 40) {
            doc.add(
                new Paragraph(
                    "... и ещё " + (products.size() - 40) + " позиций",
                    fonts.regular(9)
                )
            );
        }
        doc.add(table);
        doc.add(new Paragraph(" ", fonts.regular(8)));
    }

    private void addDailyChart(Document doc, List<DailyAggregationDTO> daily)
        throws DocumentException {
        Map<LocalDate, Long> dateToQty = new LinkedHashMap<>();
        Map<LocalDate, Long> dateToDiff = new LinkedHashMap<>();
        for (DailyAggregationDTO d : daily) {
            dateToQty.put(d.getScanDate(), d.getTotalQuantity());
            dateToDiff.put(d.getScanDate(), d.getTotalAbsDifference());
        }
        Image chart = chartRenderer.renderDailyAggregationChart(
            dateToQty,
            dateToDiff,
            "Ежедневная динамика: количество и |расхождение|"
        );
        chart.scalePercent(80);
        chart.setAlignment(Image.ALIGN_CENTER);
        doc.add(chart);
        doc.add(new Paragraph(" ", fonts.regular(4)));
    }

    private void addDiscrepancyChart(
        Document doc,
        List<DiscrepancySummaryDTO> discrepancies
    ) throws DocumentException {
        List<Map.Entry<String, Long>> top = discrepancies
            .stream()
            .sorted((a, b) ->
                Long.compare(
                    b.getTotalAbsDiscrepancy(),
                    a.getTotalAbsDiscrepancy()
                )
            )
            .limit(10)
            .map(d -> Map.entry(d.getSkuCode(), d.getTotalAbsDiscrepancy()))
            .collect(Collectors.toList());
        Image chart = chartRenderer.renderTopDiscrepancyChart(
            top,
            "Топ-10 продуктов по |расхождению| за 30 дней"
        );
        chart.scalePercent(80);
        chart.setAlignment(Image.ALIGN_CENTER);
        doc.add(chart);
        doc.add(new Paragraph(" ", fonts.regular(4)));
    }

    private void addDiscrepancyTable(
        Document doc,
        List<DiscrepancySummaryDTO> discrepancies
    ) throws DocumentException {
        doc.add(
            new Paragraph("Детализация расхождений (30 дней)", fonts.bold(14))
        );
        doc.add(new Paragraph(" ", fonts.regular(4)));

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 22, 22, 14, 14, 14, 14 });

        String[] headers = {
            "SKU",
            "Название",
            "Сканов",
            "∑|расх.|",
            "Среднее",
            "Макс.",
        };
        for (String h : headers) {
            table.addCell(headerCell(h));
        }
        int limit = Math.min(discrepancies.size(), 20);
        for (int i = 0; i < limit; i++) {
            DiscrepancySummaryDTO d = discrepancies.get(i);
            table.addCell(cell(d.getSkuCode(), 8));
            table.addCell(cell(ellipsis(d.getProductName(), 20), 8));
            table.addCell(cell(String.valueOf(d.getScanCount()), 8));
            table.addCell(cell(String.valueOf(d.getTotalAbsDiscrepancy()), 8));
            table.addCell(
                cell(
                    d.getAvgDiscrepancy() != null
                        ? String.format("%.1f", d.getAvgDiscrepancy())
                        : "-",
                    8
                )
            );
            table.addCell(cell(String.valueOf(d.getMaxDiscrepancy()), 8));
        }
        doc.add(table);
    }

    private void addProductHistoryCharts(
        Document doc,
        Map<String, List<ProductDailyHistoryDTO>> productHistory
    ) throws DocumentException {
        doc.add(
            new Paragraph(
                "Графики по запрошенным товарам (" +
                    productHistory.size() +
                    " SKU)",
                fonts.bold(14)
            )
        );
        doc.add(new Paragraph(" ", fonts.regular(6)));

        int skuIndex = 0;
        for (var entry : productHistory.entrySet()) {
            String sku = entry.getKey();
            List<ProductDailyHistoryDTO> history = entry.getValue();

            Map<LocalDate, Long> dateToQty = new LinkedHashMap<>();
            Map<LocalDate, Long> dateToDiff = new LinkedHashMap<>();
            for (ProductDailyHistoryDTO h : history) {
                dateToQty.put(h.getScanDate(), h.getDailyQuantity());
                dateToDiff.put(h.getScanDate(), h.getDailyDifference());
            }

            PdfPTable pairTable = new PdfPTable(2);
            pairTable.setWidthPercentage(100);

            Image qtyChart = chartRenderer.renderProductQuantityLineChart(
                sku,
                dateToQty
            );
            qtyChart.scalePercent(48);
            PdfPCell leftCell = new PdfPCell(qtyChart, false);
            leftCell.setBorder(PdfPCell.NO_BORDER);
            pairTable.addCell(leftCell);

            Image diffChart = chartRenderer.renderProductDifferenceLineChart(
                sku,
                dateToDiff
            );
            diffChart.scalePercent(48);
            PdfPCell rightCell = new PdfPCell(diffChart, false);
            rightCell.setBorder(PdfPCell.NO_BORDER);
            pairTable.addCell(rightCell);

            doc.add(pairTable);
            doc.add(new Paragraph(" ", fonts.regular(6)));

            skuIndex++;
            if (skuIndex < productHistory.size()) {
                doc.add(new Paragraph(" ", fonts.regular(2)));
            }
        }
    }

    private void addKeyValue(PdfPTable table, String key, String value) {
        table.addCell(new PdfPCell(new Paragraph(key, fonts.regular(10))));
        table.addCell(new PdfPCell(new Paragraph(value, fonts.regular(10))));
    }

    private PdfPCell headerCell(String text) {
        PdfPCell c = new PdfPCell(new Paragraph(text, fonts.bold(8)));
        c.setBackgroundColor(BaseColor.LIGHT_GRAY);
        return c;
    }

    private PdfPCell cell(String text, float fontSize) {
        return new PdfPCell(
            new Paragraph(text != null ? text : "-", fonts.regular(fontSize))
        );
    }

    private String nvl(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String ellipsis(String text, int maxLen) {
        if (text == null) return "-";
        return text.length() > maxLen
            ? text.substring(0, maxLen - 1) + "…"
            : text;
    }
}
