package ru.rtc.warehouse.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.auth.UserDetailsImpl;
import ru.rtc.warehouse.auth.controller.dto.request.RegisterRequest;
import ru.rtc.warehouse.auth.controller.dto.response.AuthResponse;
import ru.rtc.warehouse.auth.model.RefreshToken;
import ru.rtc.warehouse.auth.repository.RefreshTokenRepository;
import ru.rtc.warehouse.auth.util.JwtUtil;
import ru.rtc.warehouse.user.model.User;
import ru.rtc.warehouse.user.repository.UserRepository;


import java.time.Instant;
import java.util.*;

@Service
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtUtil jwtUtil;
	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final long refreshTokenValiditySeconds;

	public AuthService(
			AuthenticationManager authenticationManager,
			JwtUtil jwtUtil,
			UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			@Value("${security.jwt.refresh-token-exp-seconds:1209600}") long refreshTokenValiditySeconds // default 14 days
	) {
		this.authenticationManager = authenticationManager;
		this.jwtUtil = jwtUtil;
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
	}

	public AuthResponse login(String email, String password) {
		Authentication auth = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(email, password)
		);

		var userDetails = (UserDetailsImpl) auth.getPrincipal();
		User user = userDetails.getUser();

		String accessToken = createAccessToken(user);
		RefreshToken refreshToken = createAndSaveRefreshToken(user);

		return AuthResponse.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken.getToken())
				.accessTokenExpiresInSeconds(jwtUtil == null ? 0 : jwtUtil.parseAndValidate(accessToken).getBody().getExpiration().getTime()/1000)
				.build();
	}

	private String createAccessToken(User user) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("roles", user.getRole().name());
		return jwtUtil.generateAccessToken(user.getEmail(), claims);
	}

	private RefreshToken createAndSaveRefreshToken(User user) {
		String token = UUID.randomUUID() + "-" + UUID.randomUUID();
		Instant expiry = Instant.now().plusSeconds(refreshTokenValiditySeconds);
		RefreshToken rt = RefreshToken.builder()
				.token(token)
				.user(user)
				.expiryDate(expiry)
				.revoked(false)
				.build();
		return refreshTokenRepository.save(rt);
	}

	public AuthResponse refreshAccessToken(String refreshTokenString) {
		RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
				.orElseThrow(() -> new RuntimeException("Invalid refresh token"));

		if (refreshToken.isRevoked() || refreshToken.getExpiryDate().isBefore(Instant.now())) {
			throw new RuntimeException("Refresh token expired or revoked");
		}

		User user = refreshToken.getUser();

		refreshToken.setRevoked(true);
		refreshTokenRepository.save(refreshToken);

		RefreshToken newRefresh = createAndSaveRefreshToken(user);
		String accessToken = createAccessToken(user);

		return AuthResponse.builder()
				.accessToken(accessToken)
				.refreshToken(newRefresh.getToken())
				.accessTokenExpiresInSeconds(jwtUtil == null ? 0 : jwtUtil.parseAndValidate(accessToken).getBody().getExpiration().getTime()/1000)
				.build();
	}

	public void logout(String refreshToken) {
		refreshTokenRepository.findByToken(refreshToken).ifPresent(rt -> {
			rt.setRevoked(true);
			refreshTokenRepository.save(rt);
		});
	}

	public void register(RegisterRequest request) {

		String email = request.getEmail();
		String password = request.getPassword();
		String name = request.getName();
		User.Role role = request.getRole();

		if (userRepository.findByEmail(email).isPresent()) {
			throw new RuntimeException("User already exists");
		}
		User u = User.builder()
				.email(email)
				.passwordHash(passwordEncoder.encode(password))
				.name(name)
				.role(role)
				.createdAt(java.time.LocalDateTime.now())
				.build();
		User user = userRepository.save(u);

		String accessToken = createAccessToken(user);
		RefreshToken refreshToken = createAndSaveRefreshToken(user);

		AuthResponse.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken.getToken())
				.accessTokenExpiresInSeconds(
						jwtUtil.parseAndValidate(accessToken).getBody().getExpiration().getTime() / 1000
				)
				.build();
	}
}
