package com.sirp.incident.incident.helper;

import com.sirp.incident.exception.InactiveUserException;
import com.sirp.incident.exception.UserNotFoundException;
import com.sirp.incident.integration.user.UserClient;
import com.sirp.incident.integration.user.dto.UserResponse;
import com.sirp.incident.integration.user.fallback.UserClientFallback;
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
     * config-server's incident-service.yml). UserNotFoundException/
     * InactiveUserException are business outcomes of a successful call,
     * not call failures, so they're listed in that instance's
     * ignore-exceptions and don't count against the breaker or trigger
     * validateFallback.
     */
    @CircuitBreaker(name = "user-service", fallbackMethod = "validateFallback")
    public void validate(UUID userId) {
        UserResponse user = userClient.getUser(userId);
        if (user == null) {
            throw new UserNotFoundException("User not found : " + userId);
        }
        if (!user.active()) {
            throw new InactiveUserException("User is inactive");
        }
    }

    private void validateFallback(UUID userId, Throwable t) {
        log.error("user-service circuit breaker fallback for validate({}): {}", userId, t.toString());
        userClientFallback.getUser(userId);
    }
}