package com.sirp.gateway.config;

import java.net.InetSocketAddress;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * The Gateway is the one place in this system that sees the caller's real
 * IP (every domain service sits behind it and only ever sees the Gateway's
 * own address) - see api-gateway's local application.yml for the Redis
 * connection this backs. 20 req/s sustained with a burst of 40 is a
 * dev-scale default generous enough for a dashboard polling multiple
 * endpoints, but low enough to blunt a scripted brute-force/scraping run
 * against any single route - not tuned per-route since the gap this closes
 * is "nothing limits abuse of any endpoint," not a login-specific concern
 * (that's LoginAttemptService's job, keyed by email rather than IP).
 */
@Configuration
public class RateLimiterConfig {

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(20, 40, 1);
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
            .map(InetSocketAddress::getAddress)
            .map(address -> address.getHostAddress())
            .defaultIfEmpty("unknown");
    }
}
