package ru.rtc.warehouse.ai.service.dto;

import ru.rtc.warehouse.product.model.Product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AiPredicationDTO {
	private Long id;
	private Product product;
	private LocalDate predictionDate;
	private Integer daysUntilStockout;
	private Integer recommendedOrder;
	private BigDecimal confidenceScore;
	private LocalDateTime createdAt;
}
