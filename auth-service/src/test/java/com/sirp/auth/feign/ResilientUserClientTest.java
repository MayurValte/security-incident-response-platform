package com.sirp.auth.feign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sirp.auth.dto.response.UserSecurityResponse;
import com.sirp.auth.exception.UserServiceUnavailableException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResilientUserClientTest {

    @Mock
    private UserClient userClient;

    @InjectMocks
    private ResilientUserClient resilientUserClient;

    @Test
    void findByEmailDelegatesToRawFeignClient() {
        UserSecurityResponse response = new UserSecurityResponse(UUID.randomUUID(), "jdoe@sirp.local", "encoded",
            "ENGINEER", true);
        when(userClient.findByEmail("jdoe@sirp.local")).thenReturn(response);

        assertThat(resilientUserClient.findByEmail("jdoe@sirp.local")).isEqualTo(response);
    }

    @Test
    void findByIdDelegatesToRawFeignClient() {
        UUID id = UUID.randomUUID();
        UserSecurityResponse response = new UserSecurityResponse(id, "jdoe@sirp.local", "encoded", "ENGINEER",
            true);
        when(userClient.findById(id)).thenReturn(response);

        assertThat(resilientUserClient.findById(id)).isEqualTo(response);
    }

    /**
     * Fallback methods only ever run via resilience4j's AOP-woven proxy at
     * runtime, never from a direct call in this unit test - reflection is
     * the only way to exercise their body (convert-to-UserServiceUnavailableException,
     * never swallow) without a full Spring context.
     */
    @Test
    void findByEmailFallbackWrapsThrowableInServiceUnavailable() throws Exception {
        RuntimeException cause = new RuntimeException("connection refused");

        UserServiceUnavailableException thrown = invokeFallback("findByEmailFallback", "jdoe@sirp.local", cause);

        assertThat(thrown).hasCause(cause);
    }

    @Test
    void findByIdFallbackWrapsThrowableInServiceUnavailable() throws Exception {
        RuntimeException cause = new RuntimeException("timeout");

        UserServiceUnavailableException thrown = invokeFallback("findByIdFallback", UUID.randomUUID(), cause);

        assertThat(thrown).hasCause(cause);
    }

    private UserServiceUnavailableException invokeFallback(String methodName, Object arg, Throwable cause)
        throws NoSuchMethodException, IllegalAccessException {
        Method method = ResilientUserClient.class.getDeclaredMethod(methodName, arg.getClass(), Throwable.class);
        method.setAccessible(true);
        try {
            method.invoke(resilientUserClient, arg, cause);
            throw new AssertionError("fallback method should have thrown UserServiceUnavailableException");
        } catch (InvocationTargetException ex) {
            assertThat(ex.getCause()).isInstanceOf(UserServiceUnavailableException.class);
            return (UserServiceUnavailableException) ex.getCause();
        }
    }
}
