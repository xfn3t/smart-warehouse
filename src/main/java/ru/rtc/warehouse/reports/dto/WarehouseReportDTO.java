package ru.rtc.warehouse.reports.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import ru.rtc.warehouse.reports.dto.robot.RobotActivityReportDTO;

@Getter
@Setter
public class WarehouseReportDTO {

    private WarehouseSummaryReportDTO summary;
    private List<ProductReportDTO> products;
    private List<RobotActivityReportDTO> robots;
    private List<DailyAggregationDTO> dailyAggregation;
    private List<DiscrepancySummaryDTO> discrepancies;
    private Map<String, List<ProductDailyHistoryDTO>> productHistory;
    private LocalDateTime reportCreatedAt;
}
