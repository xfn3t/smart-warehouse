package ru.rtc.warehouse.dashboard.dto.robot;

import lombok.Getter;
import lombok.Setter;
import ru.rtc.warehouse.dashboard.dto.ScanDTO;

import java.util.List;

@Getter
@Setter
public class RobotScansDTO {
	private String robot_id;
	private List<ScanDTO> scans;
	private Integer total_count;
}