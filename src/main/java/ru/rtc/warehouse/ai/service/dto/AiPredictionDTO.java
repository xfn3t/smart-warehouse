package ru.rtc.warehouse.ai.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Setter;
import ru.rtc.warehouse.product.service.dto.ProductDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiPredictionDTO {
	private Long id;
	private ProductDTO product;
	private LocalDate predictionDate;
	private Integer daysUntilStockout;
	private Integer recommendedOrder;
	private BigDecimal confidenceScore;
	private LocalDateTime createdAt;
}