package ru.rtc.warehouse.warehouse.model;

import jakarta.persistence.*;
import lombok.*;

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

	@Column(length = 255)
	private String location;

	@Builder.Default
	@Column(name = "is_deleted", nullable = false)
	private boolean isDeleted = false;
}
