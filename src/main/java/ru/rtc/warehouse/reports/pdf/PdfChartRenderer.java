package ru.rtc.warehouse.reports.pdf;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Image;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.stereotype.Component;

@Component
public class PdfChartRenderer {

    private static final int CHART_WIDTH = 500;
    private static final int CHART_HEIGHT = 280;

    public Image renderProductQuantityLineChart(
        String skuCode,
        Map<LocalDate, Long> dateToQuantity
    ) {
        XYSeries series = new XYSeries("Количество");
        int idx = 0;
        for (var entry : dateToQuantity.entrySet()) {
            series.add(idx++, entry.getValue());
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
            skuCode + " — количество",
            "Дни",
            "Кол-во",
            dataset,
            PlotOrientation.VERTICAL,
            false,
            true,
            false
        );

        styleLineChart(chart, Color.BLUE);
        BufferedImage image = chart.createBufferedImage(
            CHART_WIDTH,
            CHART_HEIGHT
        );
        return bufferedImageToPdf(image);
    }

    public Image renderProductDifferenceLineChart(
        String skuCode,
        Map<LocalDate, Long> dateToDifference
    ) {
        XYSeries series = new XYSeries("Расхождение");
        int idx = 0;
        for (var entry : dateToDifference.entrySet()) {
            series.add(idx++, entry.getValue());
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
            skuCode + " — расхождение",
            "Дни",
            "Расхождение",
            dataset,
            PlotOrientation.VERTICAL,
            false,
            true,
            false
        );

        styleLineChart(chart, Color.RED);
        BufferedImage image = chart.createBufferedImage(
            CHART_WIDTH,
            CHART_HEIGHT
        );
        return bufferedImageToPdf(image);
    }

    public Image renderDailyAggregationChart(
        Map<LocalDate, Long> dateToQuantity,
        Map<LocalDate, Long> dateToDifference,
        String title
    ) {
        DefaultCategoryDataset quantityDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset diffDataset = new DefaultCategoryDataset();

        dateToQuantity.forEach((date, qty) ->
            quantityDataset.addValue(qty, "Кол-во", date.toString())
        );
        dateToDifference.forEach((date, diff) ->
            diffDataset.addValue(diff, "Расхождение", date.toString())
        );

        JFreeChart chart = ChartFactory.createBarChart(
            title,
            "Дата",
            "Значение",
            quantityDataset,
            PlotOrientation.VERTICAL,
            false,
            true,
            false
        );
        chart
            .getCategoryPlot()
            .getDomainAxis()
            .setCategoryLabelPositions(CategoryLabelPositions.UP_45);

        BufferedImage image = chart.createBufferedImage(
            CHART_WIDTH,
            CHART_HEIGHT
        );
        return bufferedImageToPdf(image);
    }

    public Image renderRobotStatusPieChart(
        Map<String, Integer> statusDistribution,
        String title
    ) {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        statusDistribution.forEach(dataset::setValue);
        JFreeChart chart = ChartFactory.createPieChart(
            title,
            dataset,
            true,
            true,
            false
        );
        BufferedImage image = chart.createBufferedImage(
            CHART_WIDTH,
            CHART_HEIGHT
        );
        return bufferedImageToPdf(image);
    }

    public Image renderTopDiscrepancyChart(
        List<Map.Entry<String, Long>> topProducts,
        String title
    ) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        topProducts.forEach(e ->
            dataset.addValue(e.getValue(), "Расхождение", e.getKey())
        );
        JFreeChart chart = ChartFactory.createBarChart(
            title,
            "SKU",
            "Суммарное расхождение",
            dataset,
            PlotOrientation.HORIZONTAL,
            false,
            true,
            false
        );
        BufferedImage image = chart.createBufferedImage(
            CHART_WIDTH,
            CHART_HEIGHT
        );
        return bufferedImageToPdf(image);
    }

    private void styleLineChart(JFreeChart chart, Color color) {
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, color);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesShapesVisible(0, true);
        plot.setRenderer(renderer);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    }

    private Image bufferedImageToPdf(BufferedImage bufferedImage) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            return Image.getInstance(baos.toByteArray());
        } catch (IOException | BadElementException e) {
            throw new RuntimeException(
                "Failed to convert chart to PDF image",
                e
            );
        }
    }
}
