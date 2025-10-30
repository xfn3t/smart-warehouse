package ru.rtc.warehouse.location.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.rtc.warehouse.warehouse.model.Warehouse;

@Getter
@Setter
@Entity(name = "location")
@AllArgsConstructor
@NoArgsConstructor
public class Location {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Integer zone;
	private Integer row;
	private Integer shelf;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "location_status_id", nullable = false)
	private LocationStatus locationStatus;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "warehouse_id", nullable = false)
	private Warehouse warehouse;
}
