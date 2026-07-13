package com.sirp.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Slf4j
public class CorrelationIdGatewayFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .doFinally(signalType -> {
                    String correlationId = exchange.getResponse().getHeaders().getFirst(HEADER);
                    if (correlationId == null || correlationId.isBlank()) {
                        correlationId = UUID.randomUUID().toString();
                        exchange.getResponse().getHeaders().set(HEADER, correlationId);
                    }
                    log.info("{} {} [correlationId={}]", exchange.getRequest().getMethod(),
                            exchange.getRequest().getPath(), correlationId);
                });
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
