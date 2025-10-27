package ru.rtc.warehouse.robot.controller.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
public class RobotStatusRequest {

    @NotBlank
    private String robotId; // RB-001

    @NotNull
    private Instant timestamp;

    @NotBlank
    private String status; // CONNECTED | RECONNECTING | OFFLINE

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private Double batteryLevel;

    // optional
    private Instant lastDataSent;
}
