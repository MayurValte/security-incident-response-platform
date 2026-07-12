package com.sirp.gateway.config;

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
 */
@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("auth-service", r -> r.path("/api/v1/auth/**")
                .uri("http://localhost:8082"))
            .route("user-service-users", r -> r.path("/api/v1/users/**")
                .uri("http://localhost:8081"))
            .route("user-service-teams", r -> r.path("/api/v1/teams/**")
                .uri("http://localhost:8081"))
            .route("incident-service", r -> r.path("/api/v1/incidents/**")
                .uri("http://localhost:8083"))
            .route("workflow-service", r -> r.path("/api/v1/workflows/**")
                .uri("http://localhost:8086"))
            .route("analytics-service", r -> r.path("/api/v1/analytics/**")
                .uri("http://localhost:8087"))
            .route("playbook-service", r -> r.path("/api/v1/playbooks/**")
                .uri("http://localhost:8084"))
            .route("reporting-service", r -> r.path("/api/v1/reports/**")
                .uri("http://localhost:8085"))
            .build();
    }
}


