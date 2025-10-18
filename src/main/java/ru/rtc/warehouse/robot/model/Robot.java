package ru.rtc.warehouse.robot.model;

import jakarta.persistence.*;
import lombok.*;
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
	@Column(length = 50)
	private String id; // RB-XXX

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
