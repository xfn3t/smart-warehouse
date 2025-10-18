package ru.rtc.warehouse.auth.model;

import jakarta.persistence.*;
import lombok.*;
import ru.rtc.warehouse.user.model.User;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens", indexes = @Index(columnList = "token"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 512)
	private String token;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "expiry_date", nullable = false)
	private Instant expiryDate;

	@Column(name = "revoked", nullable = false)
	private boolean revoked = false;
}
