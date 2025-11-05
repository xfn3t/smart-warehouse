package ru.rtc.warehouse.dashboard.dto.robot;

import lombok.Getter;
import lombok.Setter;
import ru.rtc.warehouse.dashboard.dto.ScanDTO;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class RobotDashboardDTO {
	private String robot_id;
	private Integer battery_level;
	private Integer zone;
	private Integer row;
	private Integer shelf;
	private String next_checkpoint;
	private LocalDateTime timestamp;
	private List<ScanDTO> recent_scans;
	private String status;
	private String warehouse_code;
}