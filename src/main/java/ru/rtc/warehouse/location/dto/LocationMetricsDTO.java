package ru.rtc.warehouse.location.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LocationMetricsDTO {
    private String warehouseCode; // optional
    private Integer zone;
    private Integer row;
    private Integer shelf;

    private LocalDateTime lastScannedAt;
    private Integer scansCount24h;
    private Double avgIntervalMinutes;
    private Long minutesSinceLastScan;

    private String status; // RECENT | MEDIUM | OLD
}
