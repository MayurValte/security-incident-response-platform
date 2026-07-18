package com.sirp.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sirp.auth.dto.response.UserSecurityResponse;
import com.sirp.auth.feign.ResilientUserClient;
import feign.FeignException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private ResilientUserClient userClient;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void returnsUserPrincipalWrappingLookupResult() {
        UserSecurityResponse user = new UserSecurityResponse(UUID.randomUUID(), "jdoe@sirp.local", "encoded",
            "ENGINEER", true);
        when(userClient.findByEmail("jdoe@sirp.local")).thenReturn(user);

        UserDetails result = customUserDetailsService.loadUserByUsername("jdoe@sirp.local");

        assertThat(result).isInstanceOf(UserPrincipal.class);
        assertThat(result.getUsername()).isEqualTo("jdoe@sirp.local");
        assertThat(result.getPassword()).isEqualTo("encoded");
        assertThat(result.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ENGINEER");
    }

    @Test
    void convertsFeign404ToUsernameNotFound() {
        FeignException notFound = mock(FeignException.class);
        when(notFound.status()).thenReturn(404);
        when(userClient.findByEmail("missing@sirp.local")).thenThrow(notFound);

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("missing@sirp.local"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasCauseReference(notFound);
    }

    @Test
    void rethrowsFeignExceptionForNon404Status() {
        FeignException serverError = mock(FeignException.class);
        when(serverError.status()).thenReturn(503);
        when(userClient.findByEmail("jdoe@sirp.local")).thenThrow(serverError);

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("jdoe@sirp.local"))
            .isSameAs(serverError);
    }
}
