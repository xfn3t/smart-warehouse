package ru.rtc.warehouse.robot.model;

import jakarta.persistence.*;
import lombok.*;
import ru.rtc.warehouse.location.model.Location;
import ru.rtc.warehouse.warehouse.model.Warehouse;

import java.time.LocalDateTime;

@Entity
@Table(name = "robots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Robot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "robot_code", length = 50, nullable = false, unique = true)
	private String code;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "warehouse_id", nullable = false)
	private Warehouse warehouse;

	@ManyToOne(optional = false, cascade = CascadeType.MERGE)
	@JoinColumn(name = "status_id", nullable = false)
	private RobotStatus status;

	@Column(name = "battery_level")
	private Integer batteryLevel;

	@Column(name = "last_update")
	private LocalDateTime lastUpdate;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "location_id", nullable = false)
	private Location location;

	@Column(name = "is_deleted", nullable = false)
	private boolean isDeleted = false;
}
