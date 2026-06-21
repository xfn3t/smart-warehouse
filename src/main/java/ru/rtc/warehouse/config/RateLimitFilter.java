package ru.rtc.warehouse.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redis;

    @Value("${app.rate-limit.max-requests-per-second:100}")
    private int maxRequestsPerSecond;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {
        String key = "rate:" + LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        Long count = redis.opsForValue().increment(key);
        if (count == 1) {
            redis.expire(key, Duration.ofSeconds(1));
        }
        if (count > maxRequestsPerSecond) {
            response.setStatus(429);
            response.getWriter().write("Too many requests");
            return;
        }
        chain.doFilter(request, response);
    }
}
