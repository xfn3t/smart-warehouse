package ru.rtc.warehouse.warehouse.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

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
