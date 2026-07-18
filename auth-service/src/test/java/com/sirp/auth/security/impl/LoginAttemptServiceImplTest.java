package com.sirp.auth.security.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sirp.auth.config.LoginThrottleProperties;
import com.sirp.auth.exception.AccountLockedException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceImplTest {

    private static final String EMAIL = "jdoe@sirp.local";
    private static final String ATTEMPTS_KEY = "auth:login:attempts:" + EMAIL;
    private static final String LOCKOUT_KEY = "auth:login:lockout:" + EMAIL;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private LoginThrottleProperties properties;
    private LoginAttemptServiceImpl loginAttemptService;

    @BeforeEach
    void setUp() {
        properties = new LoginThrottleProperties();
        properties.setMaxAttempts(5);
        properties.setAttemptWindowSeconds(900);
        properties.setLockoutSeconds(900);
        loginAttemptService = new LoginAttemptServiceImpl(redisTemplate, properties);
    }

    @Nested
    class CheckNotLocked {

        @Test
        void doesNothingWhenNoLockoutKeyPresent() {
            when(redisTemplate.hasKey(LOCKOUT_KEY)).thenReturn(false);

            assertThatNoException().isThrownBy(() -> loginAttemptService.checkNotLocked(EMAIL));
        }

        @Test
        void throwsAccountLockedWhenLockoutKeyPresent() {
            when(redisTemplate.hasKey(LOCKOUT_KEY)).thenReturn(true);

            assertThatThrownBy(() -> loginAttemptService.checkNotLocked(EMAIL))
                .isInstanceOf(AccountLockedException.class);
        }
    }

    @Nested
    class RecordFailure {

        @BeforeEach
        void stubValueOps() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        }

        @Test
        void setsExpiryOnlyOnFirstAttempt() {
            when(valueOperations.increment(ATTEMPTS_KEY)).thenReturn(1L);

            loginAttemptService.recordFailure(EMAIL);

            verify(redisTemplate).expire(ATTEMPTS_KEY, Duration.ofSeconds(900));
            verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
        }

        @Test
        void doesNotResetExpiryOnSubsequentAttemptsBelowThreshold() {
            when(valueOperations.increment(ATTEMPTS_KEY)).thenReturn(3L);

            loginAttemptService.recordFailure(EMAIL);

            verify(redisTemplate, never()).expire(eq(ATTEMPTS_KEY), any(Duration.class));
        }

        @Test
        void locksAccountAndClearsAttemptsWhenThresholdReached() {
            when(valueOperations.increment(ATTEMPTS_KEY)).thenReturn(5L);

            loginAttemptService.recordFailure(EMAIL);

            verify(valueOperations).set(LOCKOUT_KEY, "1", Duration.ofSeconds(900));
            verify(redisTemplate).delete(ATTEMPTS_KEY);
        }

        @Test
        void doesNotLockBeforeThresholdReached() {
            when(valueOperations.increment(ATTEMPTS_KEY)).thenReturn(4L);

            loginAttemptService.recordFailure(EMAIL);

            verify(valueOperations, never()).set(eq(LOCKOUT_KEY), anyString(), any(Duration.class));
            verify(redisTemplate, never()).delete(ATTEMPTS_KEY);
        }
    }

    @Test
    void recordSuccessClearsBothAttemptsAndLockoutKeys() {
        loginAttemptService.recordSuccess(EMAIL);

        verify(redisTemplate, times(1)).delete(ATTEMPTS_KEY);
        verify(redisTemplate, times(1)).delete(LOCKOUT_KEY);
    }
}
