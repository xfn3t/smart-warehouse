package ru.rtc.warehouse.inventory.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.rtc.warehouse.product.service.dto.ProductDTO;
import ru.rtc.warehouse.warehouse.service.dto.WarehouseDTO;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryHistorySmoothedDTO {
    private String skuCode;
    private ProductDTO product;
    private WarehouseDTO warehouse;
    private List<DataPoint> dataPoints;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DataPoint {
        private String timestamp;   // ISO instant truncated to period
        private Integer quantity;   // AVG or SUM over the period
    }
}
