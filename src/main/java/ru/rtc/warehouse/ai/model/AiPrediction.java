package ru.rtc.warehouse.ai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.rtc.warehouse.product.model.Product;

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
