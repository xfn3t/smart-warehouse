package ru.rtc.warehouse.reports.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProductDailyHistoryDTO {
    private String skuCode;
    private String productName;
    private LocalDate scanDate;
    private Long dailyQuantity;
    private Long dailyDifference;
}
