package ru.rtc.warehouse.robot.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.Builder.Default;
import ru.rtc.warehouse.robot.common.enums.RobotStatus;

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

	@Default
	@Enumerated(EnumType.STRING)
	@Column(length = 50, nullable = false)
	private RobotStatus status = RobotStatus.ACTIVE;

	private Integer batteryLevel;

	private LocalDateTime lastUpdate;

	@Column(length = 10)
	private String currentZone;

	private Integer currentRow;
	private Integer currentShelf;

}
