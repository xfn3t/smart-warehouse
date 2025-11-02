package ru.rtc.warehouse.product.model;

import jakarta.persistence.*;
import lombok.*;

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
