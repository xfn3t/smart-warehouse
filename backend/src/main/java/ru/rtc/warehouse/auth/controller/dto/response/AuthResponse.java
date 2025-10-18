package ru.rtc.warehouse.auth.controller.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
	private String accessToken;
	private String refreshToken;
	private long accessTokenExpiresInSeconds;
}