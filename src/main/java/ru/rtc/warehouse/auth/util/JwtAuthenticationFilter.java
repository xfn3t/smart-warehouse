package ru.rtc.warehouse.auth.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
									HttpServletResponse response,
									FilterChain filterChain) throws ServletException, IOException {
		try {
			String header = request.getHeader("Authorization");
			String token = null;

			if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
				token = header.substring(7);
			}

			if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				if (!jwtUtil.isTokenExpired(token)) {
					String username = jwtUtil.getSubject(token);
					Collection<? extends org.springframework.security.core.GrantedAuthority> authorities =
							jwtUtil.extractAuthorities(token);

					log.debug("Authenticating user: {} with authorities: {}", username, authorities);

					User userDetails = new User(username, "", authorities);

					UsernamePasswordAuthenticationToken authToken =
							new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authToken);
				} else {
					log.warn("Token is expired");
				}
			}
		} catch (Exception ex) {
			log.error("Cannot set user authentication: {}", ex.getMessage());
		}

		filterChain.doFilter(request, response);
	}
}
