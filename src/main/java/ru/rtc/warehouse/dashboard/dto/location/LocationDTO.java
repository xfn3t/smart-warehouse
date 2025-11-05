package ru.rtc.warehouse.dashboard.dto.location;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class LocationDTO {
	private Integer zone;
	private Integer row;
	private Integer shelf;
}