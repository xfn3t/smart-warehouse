package ru.rtc.warehouse.dashboard.dto.location;

import lombok.Getter;
import lombok.Setter;
import ru.rtc.warehouse.location.dto.LocationMetricsDTO;

import java.util.List;

@Getter
@Setter
public class WarehouseLocationsDTO {
	private String warehouse_code;
	private List<LocationMetricsDTO> locations;
}

