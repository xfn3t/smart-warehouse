package ru.rtc.warehouse.auth.controller;

import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.rtc.warehouse.auth.controller.dto.request.AuthRequest;
import ru.rtc.warehouse.auth.controller.dto.request.RefreshRequest;
import ru.rtc.warehouse.auth.controller.dto.request.RegisterRequest;
import ru.rtc.warehouse.auth.controller.dto.response.AuthResponse;
import ru.rtc.warehouse.auth.service.AuthService;
import ru.rtc.warehouse.user.model.User;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;


	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.ok(authService.register(request));
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
		var resp = authService.login(request.getEmail(), request.getPassword());
		return ResponseEntity.ok(resp);
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request) {
		var resp = authService.refreshAccessToken(request.getRefreshToken());
		return ResponseEntity.ok(resp);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@RequestBody RefreshRequest request) {
		authService.logout(request.getRefreshToken());
		return ResponseEntity.noContent().build();
	}
}
