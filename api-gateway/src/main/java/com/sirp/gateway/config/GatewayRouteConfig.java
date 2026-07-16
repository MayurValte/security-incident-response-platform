package com.sirp.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
 * Target URIs come from the services.* properties in application.yml
 * (each backed by a *_SERVICE_URL env var, defaulting to localhost for a
 * host-run service) rather than being hardcoded, so the same route table
 * works whether these services run on the host or as docker-compose
 * containers. Once Consul discovery is enabled and each service is
 * registered, switch these to "lb://SERVICE-NAME" for load-balanced,
 * discovery-based routing instead.
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

    @Value("${services.auth-service.uri}")
    private String authServiceUri;

    @Value("${services.user-service.uri}")
    private String userServiceUri;

    @Value("${services.incident-service.uri}")
    private String incidentServiceUri;

    @Value("${services.workflow-service.uri}")
    private String workflowServiceUri;

    @Value("${services.analytics-service.uri}")
    private String analyticsServiceUri;

    @Value("${services.audit-service.uri}")
    private String auditServiceUri;

    @Value("${services.notification-service.uri}")
    private String notificationServiceUri;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("auth-service", r -> r.path("/api/v1/auth/**")
                .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter).setKeyResolver(ipKeyResolver)))
                .uri(authServiceUri))
            .route("user-service-users", r -> r.path("/api/v1/users/**")
                .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter).setKeyResolver(ipKeyResolver)))
                .uri(userServiceUri))
            .route("user-service-teams", r -> r.path("/api/v1/teams/**")
                .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter).setKeyResolver(ipKeyResolver)))
                .uri(userServiceUri))
            .route("incident-service", r -> r.path("/api/v1/incidents/**")
                .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter).setKeyResolver(ipKeyResolver)))
                .uri(incidentServiceUri))
            .route("workflow-service", r -> r.path("/api/v1/workflows/**")
                .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter).setKeyResolver(ipKeyResolver)))
                .uri(workflowServiceUri))
            .route("analytics-service", r -> r.path("/api/v1/analytics/**")
                .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter).setKeyResolver(ipKeyResolver)))
                .uri(analyticsServiceUri))
            .route("audit-service", r -> r.path("/api/v1/audits/**")
                .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter).setKeyResolver(ipKeyResolver)))
                .uri(auditServiceUri))
            .route("notification-service", r -> r.path("/api/v1/notifications/**")
                .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter).setKeyResolver(ipKeyResolver)))
                .uri(notificationServiceUri))
            .build();
    }
}


