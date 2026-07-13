package com.sirp.incident.incident.helper;

import com.sirp.incident.exception.InactiveUserException;
import com.sirp.incident.exception.UserNotFoundException;
import com.sirp.incident.integration.user.UserClient;
import com.sirp.incident.integration.user.dto.UserResponse;
import com.sirp.incident.integration.user.fallback.UserClientFallback;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssignmentValidator {

    private final UserClient userClient;
    private final UserClientFallback userClientFallback;

    /**
     * Protected by the "user-service" resilience4j CircuitBreaker (see
     * config-server's incident-service.yml). ignore-exceptions there only
     * excludes matching exceptions from the breaker's failure-rate count -
     * it does NOT stop the AOP aspect from invoking fallbackMethod, which
     * runs for whatever escapes validate() regardless of that list. So
     * UserNotFoundException/InactiveUserException still have to be told
     * apart from a real technical failure inside validateFallback itself,
     * or they'd get swallowed into UserServiceUnavailableException (503)
     * instead of surfacing as their own 404/400.
     *
     * A 404 from user-service surfaces as a raw FeignException.NotFound,
     * not a null return - Feign throws on non-2xx responses - so it's
     * converted to UserNotFoundException here rather than relying on a
     * null check that Feign would never actually trigger (same conversion
     * CustomUserDetailsService does in auth-service for the equivalent
     * lookup).
     */
    @CircuitBreaker(name = "user-service", fallbackMethod = "validateFallback")
    public void validate(UUID userId) {
        UserResponse user;
        try {
            user = userClient.getUser(userId);
        } catch (FeignException ex) {
            if (ex.status() == 404) {
                throw new UserNotFoundException("User not found : " + userId);
            }
            throw ex;
        }
        if (!Boolean.TRUE.equals(user.enabled())) {
            throw new InactiveUserException("User is inactive");
        }
    }

    private void validateFallback(UUID userId, Throwable t) {
        if (t instanceof UserNotFoundException || t instanceof InactiveUserException) {
            throw (RuntimeException) t;
        }
        log.error("user-service circuit breaker fallback for validate({}): {}", userId, t.toString());
        userClientFallback.getUser(userId);
    }
}