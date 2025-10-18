package ru.rtc.warehouse.inventory.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import ru.rtc.warehouse.inventory.common.InventoryHistoryStatus;
import ru.rtc.warehouse.robot.model.Robot;
import ru.rtc.warehouse.product.model.Product;

@Entity
@Table(name = "inventory_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "robot_id")
	private Robot robot;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(nullable = false)
	private Integer quantity;

	@Column(nullable = false, length = 10)
	private String zone;

	@Column(name = "row_number")
	private Integer rowNumber;

	@Column(name = "shelf_number")
	private Integer shelfNumber;

	@Enumerated(EnumType.STRING)
	@Column(length = 50)
	private InventoryHistoryStatus status;

	@Column(name = "scanned_at", nullable = false)
	private LocalDateTime scannedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

}
