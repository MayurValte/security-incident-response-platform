package com.sirp.gateway.filter;

import com.sirp.security.jwt.JwtValidationService;
import com.sirp.security.model.JwtUser;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Validates the access token for every request that passes through the
 * Gateway, using the SAME shared JwtValidationService that downstream
 * services use. Only depends on the RSA PUBLIC key (via JwtValidationService
 * -> JwtTokenParser -> JwtKeyProvider) - the Gateway can verify tokens but
 * has no way to mint them.
 *
 * On success, the original Authorization header passes through UNCHANGED
 * to the downstream service (which independently re-validates it). The
 * decoded identity is additionally forwarded as X-User-* headers as a
 * convenience. Those headers are only trustworthy if the Gateway is the
 * sole network entry point to your services - enforce that at the
 * network/mesh layer (e.g. downstream services only accept traffic from
 * the Gateway's identity), or a caller who reaches a downstream service
 * directly could forge X-User-Role.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {

    private final JwtValidationService jwtValidationService;

    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/swagger-ui",
        "/v3/api-docs",
        "/actuator/health"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing or malformed Authorization header");
        }

        String token = header.substring(7);

        try {
            JwtUser user = jwtValidationService.getUser(token);

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", user.userId() == null ? "" : user.userId().toString())
                .header("X-User-Email", user.email() == null ? "" : user.email())
                .header("X-User-Role", user.role() == null ? "" : user.role())
                .header("X-Team-Id", user.teamId() == null ? "" : user.teamId().toString())
                .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (JwtException ex) {
            log.debug("Gateway JWT validation failed: {}", ex.getMessage());
            return unauthorized(exchange, "Invalid or expired token");
        }
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        String body = "{\"status\":401,\"message\":\"" + message + "\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
            .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
