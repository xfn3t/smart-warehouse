package ru.rtc.warehouse.inventory.service.product.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductLastInventoryDTO {

    private String productCode;
    private String productName;
    private String category;
    private Integer expectedQuantity;
    private Integer actualQuantity;
    private Integer difference;
    private LocalDateTime lastScannedAt;
    private String statusCode;
    private String robotCode;
    private String imageUrl;
    private Integer zone;
    private Integer row;
    private Integer shelf;
}
