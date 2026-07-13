package com.sirp.auth.feign;

import com.sirp.auth.dto.response.UserSecurityResponse;
import com.sirp.auth.exception.UserServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Thin decorator around the raw Feign UserClient so each remote call is
 * individually protected by a resilience4j CircuitBreaker (see
 * "user-service" in config-server's auth-service.yml). Callers must
 * inject THIS class rather than UserClient directly - @CircuitBreaker
 * only intercepts calls that arrive through this bean's Spring AOP
 * proxy, which requires the call to come from a different bean, not a
 * self-invocation within one method calling another in the same class.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResilientUserClient {

    private final UserClient userClient;

    @CircuitBreaker(name = "user-service", fallbackMethod = "findByEmailFallback")
    public UserSecurityResponse findByEmail(String email) {
        return userClient.findByEmail(email);
    }

    @CircuitBreaker(name = "user-service", fallbackMethod = "findByIdFallback")
    public UserSecurityResponse findById(UUID id) {
        return userClient.findById(id);
    }

    private UserSecurityResponse findByEmailFallback(String email, Throwable t) {
        log.error("user-service circuit breaker fallback for findByEmail({}): {}", email, t.toString());
        throw new UserServiceUnavailableException("User Service unavailable", t);
    }

    private UserSecurityResponse findByIdFallback(UUID id, Throwable t) {
        log.error("user-service circuit breaker fallback for findById({}): {}", id, t.toString());
        throw new UserServiceUnavailableException("User Service unavailable", t);
    }
}
