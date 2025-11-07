package ru.rtc.warehouse.product.model;

import jakarta.persistence.*;
import lombok.*;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_warehouse",
		uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "location_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductWarehouse {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "warehouse_id", nullable = false)
	private Warehouse warehouse;

	@Column(name = "min_stock")
	private Integer minStock;

	@Column(name = "optimal_stock")
	private Integer optimalStock;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "is_deleted")
	private Boolean isDeleted = false;
}