package com.sirp.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sirp.auth.dto.request.LoginRequest;
import com.sirp.auth.dto.request.RefreshTokenRequest;
import com.sirp.auth.dto.response.AuthResponse;
import com.sirp.auth.dto.response.UserSecurityResponse;
import com.sirp.auth.entity.RefreshToken;
import com.sirp.auth.exception.AccountLockedException;
import com.sirp.auth.feign.ResilientUserClient;
import com.sirp.auth.kafka.producer.AuthEventProducer;
import com.sirp.auth.security.LoginAttemptService;
import com.sirp.auth.security.TokenIssuerService;
import com.sirp.auth.service.RefreshTokenService;
import com.sirp.common.events.AuthLoginFailedEvent;
import com.sirp.common.events.AuthLoginSucceededEvent;
import com.sirp.security.properties.JwtProperties;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private ResilientUserClient userClient;

    @Mock
    private TokenIssuerService tokenIssuerService;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthEventProducer authEventProducer;

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthServiceImpl authService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Nested
    class Login {

        @Test
        void succeedsAndIssuesTokensWhenCredentialsValid() {
            LoginRequest request = new LoginRequest("jdoe@sirp.local", "Passw0rd!");
            UserSecurityResponse user = new UserSecurityResponse(userId, "jdoe@sirp.local", "encoded",
                "ENGINEER", true);
            RefreshToken refreshToken = RefreshToken.builder().userId(userId).token("refresh-token-value")
                .expiryDate(Instant.now().plusSeconds(604800)).revoked(false).build();

            when(userClient.findByEmail("jdoe@sirp.local")).thenReturn(user);
            when(tokenIssuerService.generateAccessToken(userId, "jdoe@sirp.local", "ENGINEER"))
                .thenReturn("access-token-value");
            when(refreshTokenService.createToken(userId)).thenReturn(refreshToken);
            when(jwtProperties.getExpiration()).thenReturn(900000L);

            AuthResponse response = authService.login(request);

            assertThat(response.accessToken()).isEqualTo("access-token-value");
            assertThat(response.refreshToken()).isEqualTo("refresh-token-value");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(900000L);

            verify(loginAttemptService).checkNotLocked("jdoe@sirp.local");
            verify(loginAttemptService).recordSuccess("jdoe@sirp.local");
            verify(loginAttemptService, never()).recordFailure(any());

            ArgumentCaptor<AuthLoginSucceededEvent> eventCaptor =
                ArgumentCaptor.forClass(AuthLoginSucceededEvent.class);
            verify(authEventProducer).publishLoginSucceeded(eventCaptor.capture());
            assertThat(eventCaptor.getValue().userId()).isEqualTo(userId);
            assertThat(eventCaptor.getValue().email()).isEqualTo("jdoe@sirp.local");
            verify(authEventProducer, never()).publishLoginFailed(any());
        }

        @Test
        void recordsFailureAndPublishesEventWhenCredentialsInvalid() {
            LoginRequest request = new LoginRequest("jdoe@sirp.local", "wrong-password");
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

            verify(loginAttemptService).checkNotLocked("jdoe@sirp.local");
            verify(loginAttemptService).recordFailure("jdoe@sirp.local");
            verify(loginAttemptService, never()).recordSuccess(any());

            ArgumentCaptor<AuthLoginFailedEvent> eventCaptor = ArgumentCaptor.forClass(AuthLoginFailedEvent.class);
            verify(authEventProducer).publishLoginFailed(eventCaptor.capture());
            assertThat(eventCaptor.getValue().email()).isEqualTo("jdoe@sirp.local");
            assertThat(eventCaptor.getValue().reason()).isEqualTo("BadCredentialsException");

            verify(userClient, never()).findByEmail(any());
            verify(tokenIssuerService, never()).generateAccessToken(any(), any(), any());
            verify(refreshTokenService, never()).createToken(any());
        }

        @Test
        void shortCircuitsBeforeAuthenticationWhenAccountLocked() {
            LoginRequest request = new LoginRequest("jdoe@sirp.local", "Passw0rd!");
            org.mockito.Mockito.doThrow(new AccountLockedException("locked"))
                .when(loginAttemptService).checkNotLocked("jdoe@sirp.local");

            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountLockedException.class);

            verifyNoInteractions(authenticationManager);
            verify(loginAttemptService, never()).recordFailure(any());
            verify(authEventProducer, never()).publishLoginFailed(any());
            verifyNoInteractions(userClient);
        }
    }

    @Nested
    class Refresh {

        @Test
        void rotatesRefreshTokenAndIssuesNewAccessToken() {
            RefreshTokenRequest request = new RefreshTokenRequest("old-refresh-token");
            RefreshToken existingToken = RefreshToken.builder().userId(userId).token("old-refresh-token")
                .expiryDate(Instant.now().plusSeconds(3600)).revoked(false).build();
            UserSecurityResponse user = new UserSecurityResponse(userId, "jdoe@sirp.local", "encoded",
                "ENGINEER", true);
            RefreshToken newToken = RefreshToken.builder().userId(userId).token("new-refresh-token")
                .expiryDate(Instant.now().plusSeconds(604800)).revoked(false).build();

            when(refreshTokenService.validateToken("old-refresh-token")).thenReturn(existingToken);
            when(userClient.findById(userId)).thenReturn(user);
            when(tokenIssuerService.generateAccessToken(userId, "jdoe@sirp.local", "ENGINEER"))
                .thenReturn("new-access-token");
            when(refreshTokenService.createToken(userId)).thenReturn(newToken);
            when(jwtProperties.getExpiration()).thenReturn(900000L);

            AuthResponse response = authService.refresh(request);

            assertThat(response.accessToken()).isEqualTo("new-access-token");
            assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
            verify(refreshTokenService).createToken(userId);
        }

        @Test
        void propagatesExceptionWhenRefreshTokenInvalid() {
            RefreshTokenRequest request = new RefreshTokenRequest("bad-token");
            when(refreshTokenService.validateToken("bad-token"))
                .thenThrow(new BadCredentialsException("Refresh token not found"));

            assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BadCredentialsException.class);

            verifyNoInteractions(userClient);
            verify(tokenIssuerService, never()).generateAccessToken(any(), any(), any());
        }
    }

    @Test
    void logoutDeletesRefreshTokenForUser() {
        authService.logout(userId);

        verify(refreshTokenService).deleteToken(userId);
    }
}
