package ru.rtc.warehouse.warehouse.model;

import jakarta.persistence.*;
import lombok.*;
import ru.rtc.warehouse.inventory.model.InventoryHistory;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.product.model.ProductWarehouse;
import ru.rtc.warehouse.user.model.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 50, nullable = false, unique = true)
	private String code;

	@Column(length = 255, nullable = false)
	private String name;

	@Column(name = "zone_max_size", nullable = false)
	private Integer zoneMaxSize;

	@Column(name = "row_max_size", nullable = false)
	private Integer rowMaxSize;

	@Column(name = "shelf_max_size", nullable = false)
	private Integer shelfMaxSize;

	@Column(name = "location", length = 255)
	private String warehouseLocation;

	@OneToMany(mappedBy = "warehouse")
	private Set<Location> locations;

	@ManyToMany
	@JoinTable(
			name = "user_warehouses",
			joinColumns = @JoinColumn(name = "warehouse_id"),
			inverseJoinColumns = @JoinColumn(name = "user_id")
	)
	@Builder.Default
	private Set<User> users = new HashSet<>();

	@OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<ProductWarehouse> productParameters = new ArrayList<>();

	@OneToMany(mappedBy = "warehouse", fetch = FetchType.LAZY)
	private List<InventoryHistory> inventoryHistory = new ArrayList<>();

	@Builder.Default
	@Column(name = "is_deleted", nullable = false)
	private boolean isDeleted = false;
}
