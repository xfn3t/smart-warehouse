package ru.rtc.warehouse.auth.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.rtc.warehouse.exception.UnauthorizedException;
import ru.rtc.warehouse.auth.UserDetailsServiceImpl;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;
	private final UserDetailsServiceImpl userDetailsService;

	// Временный фильтр для обхода 403
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String header = request.getHeader("Authorization");
		String token = (StringUtils.hasText(header) && header.startsWith("Bearer ")) ? header.substring(7) : null;

		if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			try {
				String username = jwtUtil.getSubject(token);
				var userDetails = userDetailsService.loadUserByUsername(username);
				if (!jwtUtil.isTokenExpired(token)) {
					var authToken = new UsernamePasswordAuthenticationToken(
							userDetails, null, userDetails.getAuthorities());
					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authToken);
				}
			} catch (RuntimeException ex) {
				// ЛОГИРУЕМ и ПРОПУСКАЕМ дальше неаутентифицированным
				// log.warn("JWT auth failed", ex);
			}
		}
		filterChain.doFilter(request, response);
	}
}
