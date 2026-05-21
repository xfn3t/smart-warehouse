package ru.rtc.warehouse.warehouse.controller.dto.request;

import java.util.List;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WarehouseUpdateRequest {

    @Size(max = 50, message = "Code must not exceed 50 characters")
    private String code;

    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Positive(message = "Zone max size must be positive")
    private Integer zoneMaxSize;

    @Positive(message = "Row max size must be positive")
    private Integer rowMaxSize;

    @Positive(message = "Shelf max size must be positive")
    private Integer shelfMaxSize;

    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    /** Optional: null = keep, [] = clear all, non-empty = replace. */
    private List<ExcludedCellDTO> excludedCells;
}
