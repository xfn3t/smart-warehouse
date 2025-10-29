package ru.rtc.warehouse.auth.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

	private final Key key;

	@Getter
	private final long accessTokenValiditySeconds;

	public JwtUtil(
			@Value("${security.jwt.secret}") String secret,
			@Value("${security.jwt.access-token-exp-seconds:900}") long accessTokenValiditySeconds
	) {
		if (secret == null || secret.length() < 32) {
			throw new IllegalArgumentException("JWT secret must be at least 32 characters long");
		}
		this.key = Keys.hmacShaKeyFor(secret.getBytes());
		this.accessTokenValiditySeconds = accessTokenValiditySeconds;
	}

	public String generateAccessToken(String subject, Map<String, Object> claims) {
		Instant now = Instant.now();
		Instant exp = now.plusSeconds(accessTokenValiditySeconds);
		return Jwts.builder()
				.setClaims(claims)
				.setSubject(subject)
				.setIssuedAt(Date.from(now))
				.setExpiration(Date.from(exp))
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
	}

	public Jws<Claims> parseAndValidate(String token) throws JwtException {
		return Jwts.parserBuilder()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token);
	}

	public boolean isTokenExpired(String token) {
		try {
			Date exp = parseAndValidate(token).getBody().getExpiration();
			return exp.before(new Date());
		} catch (JwtException e) {
			return true;
		}
	}

	public String getSubject(String token) {
		return parseAndValidate(token).getBody().getSubject();
	}

	public Collection<? extends GrantedAuthority> extractAuthorities(String token) {
		try {
			Claims claims = parseAndValidate(token).getBody();
			Object rolesClaim = claims.get("roles");

			if (rolesClaim instanceof List<?>) {
				List<String> roles = ((List<?>) rolesClaim).stream()
						.map(Object::toString)
						.toList();

				return roles.stream()
						.map(role -> new SimpleGrantedAuthority("ROLE_" + role))
						.collect(Collectors.toList());
			}
			return List.of();
		} catch (JwtException e) {
			return List.of();
		}
	}

}
