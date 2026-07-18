package com.sirp.incident.incident.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sirp.incident.exception.InactiveUserException;
import com.sirp.incident.exception.UserNotFoundException;
import com.sirp.incident.exception.UserServiceUnavailableException;
import com.sirp.incident.integration.user.UserClient;
import com.sirp.incident.integration.user.dto.UserResponse;
import com.sirp.incident.integration.user.fallback.UserClientFallback;
import feign.FeignException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignmentValidatorTest {

    @Mock
    private UserClient userClient;

    @Mock
    private UserClientFallback userClientFallback;

    // Deliberately NOT @InjectMocks: UserClientFallback implements UserClient,
    // so both mocks are assignable to the constructor's UserClient parameter
    // and Mockito's constructor-injection matching picked the wrong one for
    // it, leaving the real userClient stub un-wired (calls fell through to
    // an unstubbed mock returning null instead of the exception/response
    // configured below). Explicit construction removes the ambiguity.
    private AssignmentValidator assignmentValidator;

    @BeforeEach
    void wireAssignmentValidator() {
        assignmentValidator = new AssignmentValidator(userClient, userClientFallback);
    }

    @Nested
    class Validate {

        @Test
        void succeedsForAnActiveUser() {
            UUID userId = UUID.randomUUID();
            when(userClient.getUser(userId))
                .thenReturn(new UserResponse(userId, "jdoe", "jdoe@sirp.local", "ENGINEER", true));

            assertThatCode(() -> assignmentValidator.validate(userId)).doesNotThrowAnyException();
        }

        @Test
        void throwsInactiveUserExceptionForADisabledUser() {
            UUID userId = UUID.randomUUID();
            when(userClient.getUser(userId))
                .thenReturn(new UserResponse(userId, "jdoe", "jdoe@sirp.local", "ENGINEER", false));

            assertThatThrownBy(() -> assignmentValidator.validate(userId))
                .isInstanceOf(InactiveUserException.class);
        }

        @Test
        void convertsFeign404ToUserNotFound() {
            UUID userId = UUID.randomUUID();
            FeignException notFound = mock(FeignException.class);
            when(notFound.status()).thenReturn(404);
            when(userClient.getUser(userId)).thenThrow(notFound);

            assertThatThrownBy(() -> assignmentValidator.validate(userId))
                .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void rethrowsNon404FeignExceptionsAsIs() {
            UUID userId = UUID.randomUUID();
            FeignException serverError = mock(FeignException.class);
            when(serverError.status()).thenReturn(500);
            when(userClient.getUser(userId)).thenThrow(serverError);

            assertThatThrownBy(() -> assignmentValidator.validate(userId))
                .isSameAs(serverError);
        }
    }

    /**
     * validateFallback only ever runs via resilience4j's AOP proxy at
     * runtime (the @CircuitBreaker annotation is inert on a direct call in
     * a plain unit test), so reflection is the only way to exercise it
     * here. Worth testing directly because a prior bug swallowed
     * UserNotFoundException/InactiveUserException into a generic 503 -
     * ignore-exceptions only excludes them from the breaker's failure-rate
     * count, it does not stop the fallback from running for them too.
     */
    @Nested
    class ValidateFallback {

        @Test
        void rethrowsUserNotFoundAsIsInsteadOfConvertingToServiceUnavailable() throws Exception {
            UUID userId = UUID.randomUUID();
            UserNotFoundException original = new UserNotFoundException("User not found : " + userId);

            InvocationTargetException wrapped = invokeFallbackExpectingThrow(userId, original);

            assertThat(wrapped.getCause()).isSameAs(original);
            verifyNoInteractions(userClientFallback);
        }

        @Test
        void rethrowsInactiveUserAsIsInsteadOfConvertingToServiceUnavailable() throws Exception {
            UUID userId = UUID.randomUUID();
            InactiveUserException original = new InactiveUserException("User is inactive");

            InvocationTargetException wrapped = invokeFallbackExpectingThrow(userId, original);

            assertThat(wrapped.getCause()).isSameAs(original);
            verifyNoInteractions(userClientFallback);
        }

        @Test
        void delegatesToUserClientFallbackForAGenuineTechnicalFailure() throws Exception {
            UUID userId = UUID.randomUUID();
            RuntimeException technicalFailure = new RuntimeException("connection refused");
            when(userClientFallback.getUser(userId))
                .thenThrow(new UserServiceUnavailableException("User Service unavailable"));

            InvocationTargetException wrapped = invokeFallbackExpectingThrow(userId, technicalFailure);

            assertThat(wrapped.getCause()).isInstanceOf(UserServiceUnavailableException.class);
            verify(userClientFallback).getUser(userId);
        }

        private InvocationTargetException invokeFallbackExpectingThrow(UUID userId, Throwable cause)
            throws NoSuchMethodException, IllegalAccessException {
            Method method = AssignmentValidator.class.getDeclaredMethod("validateFallback", UUID.class,
                Throwable.class);
            method.setAccessible(true);
            try {
                method.invoke(assignmentValidator, userId, cause);
                throw new AssertionError("validateFallback should have thrown");
            } catch (InvocationTargetException ex) {
                return ex;
            }
        }
    }
}
