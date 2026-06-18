package ru.rtc.warehouse.ai.service.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisPredictionDTO {

    private String sku;
    private Long productId;
    private Integer daysUntilStockout;
    private Integer recommendedOrder;
    private String criticalLevel;

    /** Из v1.3: confidence прогноза на 7 дней */
    private BigDecimal confidenceScore;

    private String warehouseCode;
    private Long lastUpdated;

    private Double quantity;
    private Double expectedQuantity;
    private Double difference;
    private Double minStock;
    private Double optimalStock;

    /** v1.3: 7-дневный прогноз */
    private List<ForecastDayDTO> forecast;
}
