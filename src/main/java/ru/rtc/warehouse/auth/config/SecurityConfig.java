package ru.rtc.warehouse.auth.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ru.rtc.warehouse.auth.UserDetailsServiceImpl;
import ru.rtc.warehouse.auth.repository.RobotTokenRepository;
import ru.rtc.warehouse.auth.util.JwtAuthenticationFilter;
import ru.rtc.warehouse.auth.util.JwtUtil;
import ru.rtc.warehouse.auth.util.RobotTokenAuthenticationFilter;
import ru.rtc.warehouse.config.RateLimitFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl customUserDetailsService;
    private final RobotTokenRepository robotTokenRepository;
    private final RateLimitFilter rateLimitFilter;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil, customUserDetailsService);
    }

    @Bean
    public RobotTokenAuthenticationFilter robotTokenAuthenticationFilter() {
        return new RobotTokenAuthenticationFilter(
            jwtUtil,
            robotTokenRepository
        );
    }

    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(11);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(
            List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        );
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth ->
                auth
                    // --- Public endpoints ---
                    .requestMatchers("/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers("/swagger-ui/**")
                    .permitAll()
                    .requestMatchers("/v3/swagger-ui/**")
                    .permitAll()
                    .requestMatchers("/api/auth/**")
                    .permitAll()
                    .requestMatchers("/api/images/**")
                    .permitAll()
                    .requestMatchers(
                        "/ws",
                        "/ws/**",
                        "/api/ws/dashboard",
                        "/api/ws/dashboard/**",
                        "/ws/info/**"
                    )
                    .permitAll()

                    // --- ADMIN only: warehouse CRUD ---
                    .requestMatchers(HttpMethod.POST, "/api/warehouse")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/warehouse/**")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/warehouse/**")
                    .hasRole("ADMIN")

                    // --- ADMIN only: robot registration/CRUD ---
                    .requestMatchers(HttpMethod.POST, "/api/robots/register")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/robots/**")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/robots/**")
                    .hasRole("ADMIN")

                    // --- ADMIN only: user creation ---
                    .requestMatchers(HttpMethod.POST, "/api/*/users/register")
                    .hasRole("ADMIN")

                    // --- CSV import: ADMIN + WAREHOUSE_WORKER ---
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/*/inventory/import/**"
                    )
                    .hasAnyRole("ADMIN", "WAREHOUSE_WORKER")

                    // --- Image upload/delete: ADMIN + WAREHOUSE_WORKER ---
                    .requestMatchers(HttpMethod.POST, "/api/images/**")
                    .hasAnyRole("ADMIN", "WAREHOUSE_WORKER")
                    .requestMatchers(HttpMethod.DELETE, "/api/images/**")
                    .hasAnyRole("ADMIN", "WAREHOUSE_WORKER")

                    // --- Report generation (pdf/excel): ADMIN + WAREHOUSE_WORKER ---
                    .requestMatchers(HttpMethod.POST, "/api/reports/**")
                    .hasAnyRole("ADMIN", "WAREHOUSE_WORKER")

                    // --- ALL roles (ADMIN, WAREHOUSE_WORKER, VIEWER, MANAGER, OPERATOR): GET ---
                    .requestMatchers(HttpMethod.GET, "/api/**")
                    .hasAnyRole(
                        "ADMIN",
                        "WAREHOUSE_WORKER",
                        "VIEWER",
                        "MANAGER",
                        "OPERATOR"
                    )

                    // --- Catch-all: only ADMIN and WAREHOUSE_WORKER for mutations ---
                    .anyRequest()
                    .hasAnyRole("ADMIN", "WAREHOUSE_WORKER")
            )
            .userDetailsService(customUserDetailsService);

        http.addFilterBefore(
            rateLimitFilter,
            UsernamePasswordAuthenticationFilter.class
        );
        http.addFilterBefore(
            robotTokenAuthenticationFilter(),
            UsernamePasswordAuthenticationFilter.class
        );
        http.addFilterBefore(
            jwtAuthenticationFilter(),
            UsernamePasswordAuthenticationFilter.class
        );
        return http.build();
    }
}
