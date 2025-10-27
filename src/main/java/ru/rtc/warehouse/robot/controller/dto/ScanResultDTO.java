package ru.rtc.warehouse.robot.controller.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import ru.rtc.warehouse.inventory.common.InventoryHistoryStatus;


@Getter
@Setter
public class ScanResultDTO {

    @NotNull
    private String productCode; 

    @NotBlank
    private String productName;

    @NotNull
    @Min(0)
    private Integer quantity;

    @NotNull
    private InventoryHistoryStatus status; // OK / LOW_STOCK / CRITICAL
}

