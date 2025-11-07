package ru.rtc.warehouse.product.model;

import jakarta.persistence.*;
import lombok.*;
import ru.rtc.warehouse.inventory.model.InventoryHistory;

import java.util.ArrayList;
import java.util.List;

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

	@Column(name = "sku_code", unique = true, nullable = false)
	private String skuCode;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "category")
	private String category;

	@Column(name = "is_deleted")
	private Boolean isDeleted = false;

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<ProductWarehouse> warehouseParameters = new ArrayList<>();

	@OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
	private List<InventoryHistory> inventoryHistory = new ArrayList<>();
}