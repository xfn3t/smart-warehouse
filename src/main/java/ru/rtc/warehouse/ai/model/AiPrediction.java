package ru.rtc.warehouse.ai.model;

import jakarta.persistence.*;
import lombok.*;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "warehouse_id")
	private Warehouse warehouse;

	@Column(name = "prediction_date", nullable = false)
	private LocalDate predictionDate;

	private Integer daysUntilStockout;
	private Integer recommendedOrder;

	@Column(precision = 3, scale = 2)
	private BigDecimal confidenceScore;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "is_deleted", nullable = false)
	private boolean isDeleted = false;
}
