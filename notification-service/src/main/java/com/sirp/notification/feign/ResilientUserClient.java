package com.sirp.notification.feign;

import com.sirp.notification.feign.dto.UserNotificationResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Thin decorator around the raw Feign UserClient so the remote call is
 * protected by a resilience4j CircuitBreaker (see "user-service" in
 * config-server's notification-service.yml) instead of relying solely on
 * Feign's own per-call exception handling. Callers must inject THIS
 * class rather than UserClient directly - @CircuitBreaker only
 * intercepts calls that arrive through this bean's Spring AOP proxy.
 */
@Component
@RequiredArgsConstructor
public class ResilientUserClient {

    private final UserClient userClient;
    private final UserClientFallback userClientFallback;

    @CircuitBreaker(name = "user-service", fallbackMethod = "findNotificationUserFallback")
    public UserNotificationResponse findNotificationUser(UUID id) {
        return userClient.findNotificationUser(id);
    }

    private UserNotificationResponse findNotificationUserFallback(UUID id, Throwable t) {
        return userClientFallback.findNotificationUser(id);
    }
}
