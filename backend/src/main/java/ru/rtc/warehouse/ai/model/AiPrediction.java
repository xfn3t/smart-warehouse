package ru.rtc.warehouse.ai.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import ru.rtc.warehouse.product.model.Product;

@Entity
@Table(name = "ai_predictions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiPrediction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(name = "prediction_date", nullable = false)
	private LocalDate predictionDate;

	@Column(name = "days_until_stockout")
	private Integer daysUntilStockout;

	@Column(name = "recommended_order")
	private Integer recommendedOrder;

	@Column(name = "confidence_score", precision = 3, scale = 2)
	private BigDecimal confidenceScore;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();
}
