package ru.rtc.warehouse.inventory.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventory_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryHistoryStatus {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(length = 50, nullable = false, unique = true)
	private InventoryHistoryStatusCode code;

	@Builder.Default
	@Column(name = "is_deleted", nullable = false)
	private boolean isDeleted = false;

	public enum InventoryHistoryStatusCode {
		OK,
		LOW_STOCK,
		CRITICAL;

		@JsonCreator
		public static InventoryHistoryStatusCode from(String value) {
			if (value == null) return null;
			try {
				return InventoryHistoryStatusCode.valueOf(value.trim().toUpperCase());
			} catch (IllegalArgumentException ex) {
				throw new IllegalArgumentException("Unknown status: " + value);
			}
		}
	}
}
