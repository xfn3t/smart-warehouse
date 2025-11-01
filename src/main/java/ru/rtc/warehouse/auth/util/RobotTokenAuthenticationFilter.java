package ru.rtc.warehouse.auth.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.rtc.warehouse.auth.repository.RobotTokenRepository;

import java.io.IOException;
import java.util.Collection;

@RequiredArgsConstructor
@Slf4j
public class RobotTokenAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RobotTokenRepository robotTokenRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            final String token; 

            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
                token = header.substring(7);
            } else {
                token = null;
            }


            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                try {
                    jwtUtil.parseAndValidate(token);
                } catch (Exception ex) {
                    log.debug("Robot JWT parse/validate failed: {}", ex.getMessage());
                    filterChain.doFilter(request, response);
                    return;
                }

               
                robotTokenRepository.findByTokenAndRevokedFalse(token).ifPresent(rt -> {
                    try {
                        String subject = jwtUtil.getSubject(token);
                        Collection<? extends GrantedAuthority> authorities = jwtUtil.extractAuthorities(token);

                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(subject, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } catch (Exception e) {
                        log.warn("Failed to create auth for robot token: {}", e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            log.error("RobotTokenAuthenticationFilter failure: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}