package ru.rtc.warehouse.robot.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.*;
import lombok.*;
import ru.rtc.warehouse.inventory.model.InventoryHistoryStatus;
import ru.rtc.warehouse.robot.common.StatusCodeConverter;

@Entity
@Table(name = "robot_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RobotStatus {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(length = 50, nullable = false, unique = true)
	private StatusCode code;

	@Column(name = "is_deleted", nullable = false)
	private boolean isDeleted = false;

	public enum StatusCode {
		IDLE,
		WORKING,
		CHARGING,
		ERROR;

		@JsonCreator
		public static StatusCode from(String value) {
			if (value == null) return null;
			try {
				return StatusCode.valueOf(value.trim().toUpperCase());
			} catch (IllegalArgumentException ex) {
				throw new IllegalArgumentException("Unknown status: " + value);
			}
		}
	}
}