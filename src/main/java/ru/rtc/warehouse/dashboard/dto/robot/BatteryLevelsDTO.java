package ru.rtc.warehouse.dashboard.dto.robot;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BatteryLevelsDTO {
	private Integer average;
	private Integer lowest;
	private Integer highest;
}