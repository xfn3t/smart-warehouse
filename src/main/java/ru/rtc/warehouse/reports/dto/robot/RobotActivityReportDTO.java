package ru.rtc.warehouse.reports.dto.robot;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RobotActivityReportDTO {
	private Long robotId;
	private String robotCode;
	private String warehouseCode;
	private String warehouseName;
	private String robotStatus;
	private Integer batteryLevel;
	private LocalDateTime lastUpdate;
	private Long totalScans;
	private LocalDateTime lastScanAt;
	private Long okScans;
	private Long lowStockScans;
	private Long criticalScans;
	private Long totalDifference;
}
