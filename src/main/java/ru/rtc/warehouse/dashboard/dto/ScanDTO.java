package ru.rtc.warehouse.dashboard.dto;

import lombok.Getter;
import lombok.Setter;
import ru.rtc.warehouse.location.dto.LocationDTO;

import java.time.LocalDateTime;

@Getter
@Setter
public class ScanDTO {
	private String productCode;
	private String productName;
	private Integer quantity;
	private String status;
	private Integer diff;
	private LocalDateTime scannedAt;
	private LocationDTO location;
}