package ru.rtc.warehouse.warehouse.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseDTO {
	private Long id;
	private String code;
	private String name;
	private Integer zoneMaxSize;
	private Integer rowMaxSize;
	private Integer shelfMaxSize;
	private String location;
}