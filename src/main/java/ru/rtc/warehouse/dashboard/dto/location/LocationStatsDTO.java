package ru.rtc.warehouse.dashboard.dto.location;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class LocationStatsDTO {
	private Integer total_items;
	private Integer scanned_today;
	private Integer low_stock_items;
	private Integer out_of_stock_items;
}