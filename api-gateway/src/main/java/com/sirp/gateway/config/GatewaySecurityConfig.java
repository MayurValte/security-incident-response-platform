package com.sirp.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * NEW. With spring-boot-starter-security on the classpath and no
 * SecurityWebFilterChain bean, Spring Boot auto-configures a default
 * reactive security chain: a generated login password, CSRF protection,
 * and "authenticated" required on every request - all enforced BEFORE
 * Gateway's own GlobalFilters run.
 *
 * Token validation here is deliberately done by
 * JwtAuthenticationGatewayFilter (a GlobalFilter, ordered to run early
 * in the Gateway's own filter chain), not by Spring Security's filter
 * chain. This config disables Spring Security's default behavior so it
 * gets out of the way entirely and isn't doing redundant (and
 * differently-configured) enforcement of its own.
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
            .build();
    }
}
