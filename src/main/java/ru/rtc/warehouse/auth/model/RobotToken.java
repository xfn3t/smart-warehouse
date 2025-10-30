package ru.rtc.warehouse.auth.model;

import jakarta.persistence.*;
import lombok.*;
import ru.rtc.warehouse.robot.model.Robot;

import java.time.LocalDateTime;

@Entity
@Table(name = "robot_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RobotToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "robot_id", nullable = false)
	private Robot robot;

	@Column(nullable = false, unique = true, columnDefinition = "TEXT")
	private String token;

	@Builder.Default
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Builder.Default
	@Column(nullable = false)
	private boolean revoked = false;
}
