package ru.rtc.warehouse.dashboard.service.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.robot.service.RobotEntityService;

import java.util.List;

@Service("dashboardRobotEntServiceAdapter")
@RequiredArgsConstructor
public class DashboardRobotEntServiceAdapter {

	private final RobotEntityService robotService;

	public List<Robot> findAll() {
		return robotService.findAll();
	}
}
