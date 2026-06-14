package ru.rtc.warehouse.robot.controller.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import ru.rtc.warehouse.robot.controller.dto.ScanResultDTO;

@Getter
@Setter
public class RobotDataRequest {

    @NotBlank
    @Pattern(regexp = "RB-\\d{4}", message = "Code must follow pattern RB-XXXХ")
    private String code;

    @NotNull
    private Instant timestamp;

    @Valid
    @NotNull
    private LocationDTO location;

    @Valid
    @NotNull
    @Size(min = 1)
    private List<ScanResultDTO> scanResults;

    @NotNull
    @Min(value = 0, message = "Battery level must be between 0 and 100")
    @Max(value = 100, message = "Battery level must be between 0 and 100")
    private Integer batteryLevel;

    @NotBlank
    private String nextCheckpoint;

    @Getter
    @Setter
    public static class LocationDTO {

        @NotNull
        private Integer zone;

        @NotNull
        private Integer row;

        @NotNull
        private Integer shelf;
    }
}
