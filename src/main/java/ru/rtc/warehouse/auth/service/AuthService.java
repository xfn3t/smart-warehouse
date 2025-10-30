package ru.rtc.warehouse.auth.service;

import lombok.RequiredArgsConstructor;
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
import ru.rtc.warehouse.user.controller.dto.request.UserCreateRequest;
import ru.rtc.warehouse.user.mapper.UserMapper;
import ru.rtc.warehouse.user.model.User;
import ru.rtc.warehouse.user.service.UserService;
import ru.rtc.warehouse.user.service.dto.UserDTO;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final JwtUtil jwtUtil;
	private final UserService userService;
	private final UserMapper userMapper;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${security.jwt.refresh-token-exp-seconds:1209600}")
	private long refreshTokenValiditySeconds; // default 14 days

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
		claims.put("roles", user.getRole().getCode());
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

	public AuthResponse register(RegisterRequest request) {
		UserCreateRequest createRequest = UserCreateRequest.builder()
				.email(request.getEmail())
				.name(request.getName())
				.password(passwordEncoder.encode(request.getPassword()))
				.role(request.getRole())
				.build();

		// Сохраняем пользователя и получаем UserDTO
		UserDTO userDTO = userService.save(createRequest);

		// Преобразуем UserDTO обратно в User entity для создания токена
		User user = userMapper.toEntity(userDTO);

		String accessToken = createAccessToken(user);
		RefreshToken refreshToken = createAndSaveRefreshToken(user);

		return AuthResponse.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken.getToken())
				.accessTokenExpiresInSeconds(
						jwtUtil.parseAndValidate(accessToken).getBody().getExpiration().getTime() / 1000
				)
				.build();
	}
}