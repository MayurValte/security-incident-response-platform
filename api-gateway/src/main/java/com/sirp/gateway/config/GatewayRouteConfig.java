package com.sirp.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Example route table - replace with your actual SIRP microservices.
 *
 * NOTE: there is deliberately NO route for /internal/** here. Those
 * endpoints (InternalUserController) are for service-to-service Feign
 * calls only and must never be reachable through the Gateway or from
 * outside your network - see user-service's SecurityConfig for why they
 * can't be protected by a JWT check the way public endpoints are.
 *
 * Uses plain http://host:port URIs by default so the Gateway is testable
 * immediately, without Consul running. Once Consul discovery is enabled
 * and each service is registered, switch these to "lb://SERVICE-NAME"
 * for load-balanced, discovery-based routing instead of hardcoded hosts.
 *
 * Every route carries the same requestRateLimiter filter (RateLimiterConfig's
 * redisRateLimiter/ipKeyResolver beans) - see that class for why a single
 * IP-keyed limit across all routes, rather than per-route tuning.
 */
@Configuration
@RequiredArgsConstructor
public class GatewayRouteConfig {

    private final RedisRateLimiter redisRateLimiter;
    private final KeyResolver ipKeyResolver;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("auth-service", r -> r.path("/api/v1/auth/**")
                .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter).setKeyResolver(ipKeyResolver)))
                .uri("http://localhost:8082"))
            .route("user-service-users", r -> r.path("/api/v1/users/**")
                .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter).setKeyResolver(ipKeyResolver)))
                .uri("http://localhost:8081"))
            .route("user-service-teams", r -> r.path("/api/v1/teams/**")
                .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter).setKeyResolver(ipKeyResolver)))
                .uri("http://localhost:8081"))
            .route("incident-service", r -> r.path("/api/v1/incidents/**")
                .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter).setKeyResolver(ipKeyResolver)))
                .uri("http://localhost:8083"))
            .route("workflow-service", r -> r.path("/api/v1/workflows/**")
                .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter).setKeyResolver(ipKeyResolver)))
                .uri("http://localhost:8086"))
            .route("analytics-service", r -> r.path("/api/v1/analytics/**")
                .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter).setKeyResolver(ipKeyResolver)))
                .uri("http://localhost:8087"))
            .route("audit-service", r -> r.path("/api/v1/audits/**")
                .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter).setKeyResolver(ipKeyResolver)))
                .uri("http://localhost:8084"))
            .route("notification-service", r -> r.path("/api/v1/notifications/**")
                .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter).setKeyResolver(ipKeyResolver)))
                .uri("http://localhost:8085"))
            .build();
    }
}


