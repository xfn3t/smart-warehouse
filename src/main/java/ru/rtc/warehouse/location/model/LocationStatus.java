package ru.rtc.warehouse.location.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "location_status")
@NoArgsConstructor
@AllArgsConstructor
public class LocationStatus {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "code")
	private LocationStatusCode statusCode;

	public enum LocationStatusCode {
		OLD,
		MEDIUM,
		RECENT
	}

	@JsonCreator
	public static LocationStatusCode from(String value) {
		if (value == null) return null;
		try {
			return LocationStatusCode.valueOf(value.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Unknown status: " + value);
		}
	}
}
