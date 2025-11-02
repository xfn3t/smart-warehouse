package ru.rtc.warehouse.user.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(length = 50, nullable = false, unique = true)
	private RoleCode code;

	@Builder.Default
	@Column(name = "is_deleted", nullable = false)
	private boolean isDeleted = false;

	public enum RoleCode {
		VIEWER,
		ADMIN,
		MANAGER,
		OPERATOR,
		ROBOT;

		@JsonCreator
		public static RoleCode from(String value) {
			if (value == null) return null;
			try {
				return RoleCode.valueOf(value.trim().toUpperCase());
			} catch (IllegalArgumentException ex) {
				throw new IllegalArgumentException("Unknown status: " + value);
			}
		}
	}
}
