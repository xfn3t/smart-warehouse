package ru.rtc.warehouse.warehouse.service.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.rtc.warehouse.warehouse.controller.dto.request.ExcludedCellDTO;

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
    private List<ExcludedCellDTO> excludedCells;
}
