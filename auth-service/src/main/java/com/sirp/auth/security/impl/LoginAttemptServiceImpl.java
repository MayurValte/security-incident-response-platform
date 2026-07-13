package com.sirp.auth.security.impl;

import com.sirp.auth.config.LoginThrottleProperties;
import com.sirp.auth.exception.AccountLockedException;
import com.sirp.auth.security.LoginAttemptService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Brute-force protection, keyed by email rather than IP - auth-service sits
 * behind api-gateway and never sees the caller's real IP. Attempt counts and
 * lockouts live in Redis (not the DB) since both are short-lived, high-churn
 * counters that should expire on their own via TTL rather than needing a
 * cleanup job.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private static final String ATTEMPTS_KEY_PREFIX = "auth:login:attempts:";
    private static final String LOCKOUT_KEY_PREFIX = "auth:login:lockout:";

    private final StringRedisTemplate redisTemplate;
    private final LoginThrottleProperties properties;

    @Override
    public void checkNotLocked(String email) {
        Boolean locked = redisTemplate.hasKey(LOCKOUT_KEY_PREFIX + email);
        if (Boolean.TRUE.equals(locked)) {
            throw new AccountLockedException(
                "Account temporarily locked due to repeated failed login attempts. Try again later.");
        }
    }

    @Override
    public void recordFailure(String email) {
        String attemptsKey = ATTEMPTS_KEY_PREFIX + email;
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(attemptsKey, Duration.ofSeconds(properties.getAttemptWindowSeconds()));
        }
        if (attempts != null && attempts >= properties.getMaxAttempts()) {
            redisTemplate.opsForValue().set(
                LOCKOUT_KEY_PREFIX + email, "1", Duration.ofSeconds(properties.getLockoutSeconds()));
            redisTemplate.delete(attemptsKey);
        }
    }

    @Override
    public void recordSuccess(String email) {
        redisTemplate.delete(ATTEMPTS_KEY_PREFIX + email);
        redisTemplate.delete(LOCKOUT_KEY_PREFIX + email);
    }
}
