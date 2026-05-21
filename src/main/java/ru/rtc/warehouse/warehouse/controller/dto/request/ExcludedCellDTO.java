package ru.rtc.warehouse.warehouse.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for a single excluded cell in the warehouse matrix.
 * The shelf dimension is never excluded — only (zone, row) pairs.
 */
@Getter
@Setter
public class ExcludedCellDTO {

    @NotNull
    @Positive
    private Integer zone;

    @NotNull
    @Positive
    private Integer row;
}
