package ru.rtc.warehouse.inventory.service.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.rtc.warehouse.product.service.dto.ProductDTO;
import ru.rtc.warehouse.warehouse.service.dto.WarehouseDTO;

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
    private Integer currentQuantity;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DataPoint {

        private String timestamp;
        private Integer quantity;
    }
}
