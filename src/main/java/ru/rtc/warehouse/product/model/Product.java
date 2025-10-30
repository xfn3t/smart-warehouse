package ru.rtc.warehouse.product.model;

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
import ru.rtc.warehouse.warehouse.model.Warehouse;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "warehouse_id", nullable = false)
	private Warehouse warehouse;

	@Column(name = "sku_code", length = 50, nullable = false, unique = true)
	private String code;

	@Column(nullable = false, length = 255)
	private String name;

	@Column(length = 100)
	private String category;

	@Column(name = "min_stock", nullable = false)
	@Builder.Default
	private Integer minStock = 10;

	@Column(name = "optimal_stock", nullable = false)
	@Builder.Default
	private Integer optimalStock = 100;

	@Column(name = "is_deleted", nullable = false)
	@Builder.Default
	private boolean isDeleted = false;
}
