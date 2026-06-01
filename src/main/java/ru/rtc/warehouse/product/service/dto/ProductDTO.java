package ru.rtc.warehouse.product.service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ProductDTO {
    private Long id;
    private String code;
    private String name;
    private String category;
    private String imageUrl;
    private String description;
    private List<ProductWarehouseDTO> warehouseParameters;
}
