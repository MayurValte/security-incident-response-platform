package com.sirp.audit.config;

import com.sirp.security.filter.CorrelationIdFilter;
import com.sirp.security.filter.JwtAuthenticationFilter;
import com.sirp.security.handler.RestAccessDeniedHandler;
import com.sirp.security.handler.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Defense-in-depth: re-validates the JWT independently of the Gateway.
 * No /internal/** exception here - audit-service is populated entirely
 * via Kafka consumers, not Feign, so every HTTP endpoint is a public,
 * gateway-routed read.
 *
 * /actuator/prometheus is public (like /actuator/health) rather than
 * ADMIN-gated with the rest of /actuator/**, because docker-compose's
 * Prometheus service scrapes it via a plain static target with no way
 * to attach a JWT (tokens expire in 15 minutes; a static scrape config
 * can't refresh one). Only prometheus/health are exempted - everything
 * else under /actuator/** (metrics, circuitbreakers, etc.) still
 * requires ADMIN.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorrelationIdFilter correlationIdFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(correlationIdFilter, JwtAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/actuator/health",
                    "/actuator/prometheus")
                .permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .build();
    }
}
