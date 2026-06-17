package ru.rtc.warehouse.product.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Товар пользователя с информацией о складе и актуальным количеством")
public class UserProductOnWarehouseDTO {

    @Schema(description = "SKU-код товара", example = "A1B2C3D4E5")
    private String skuCode;

    @Schema(description = "Название товара", example = "Наушники беспроводные")
    private String productName;

    @Schema(description = "Категория товара", example = "Электроника")
    private String category;

    @Schema(description = "URL изображения товара", example = "https://example.com/images/headphones.jpg")
    private String imageUrl;

    @Schema(description = "Код склада", example = "WH01")
    private String warehouseCode;

    @Schema(description = "Название склада", example = "Центральный склад")
    private String warehouseName;

    @Schema(description = "Актуальное количество товара на складе (последнее сканирование)", example = "42")
    private Integer quantity;

    @Schema(description = "Минимальный запас", example = "10")
    private Integer minStock;

    @Schema(description = "Оптимальный запас", example = "50")
    private Integer optimalStock;

    @Schema(description = "Зона на складе", example = "1")
    private Integer zone;

    @Schema(description = "Ряд на складе", example = "3")
    private Integer row;

    @Schema(description = "Полка на складе", example = "2")
    private Integer shelf;
}
