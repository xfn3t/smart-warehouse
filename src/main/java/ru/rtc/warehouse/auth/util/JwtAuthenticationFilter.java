package ru.rtc.warehouse.auth.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.rtc.warehouse.auth.UserDetailsServiceImpl;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;
	private final UserDetailsServiceImpl userDetailsService;

	// Временный фильтр для обхода 403
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

					UserDetails userDetails = userDetailsService.loadUserByUsername(username);

					log.debug("Authenticating user: {} with authorities: {}", username, userDetails.getAuthorities());

					UsernamePasswordAuthenticationToken authToken =
							new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
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