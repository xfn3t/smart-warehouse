package ru.rtc.warehouse.robot.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class LocationDTO {
    @NotNull
    @Min(0)
    private Integer zone;

    @NotNull
    @Min(0)
    private Integer row;

    @NotNull
    @Min(0)
    private Integer shelf;
}

