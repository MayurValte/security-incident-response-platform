package com.sirp.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sirp.auth.entity.RefreshToken;
import com.sirp.auth.repository.RefreshTokenRepository;
import com.sirp.security.properties.JwtProperties;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository repository;

    @Mock
    private JwtProperties properties;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Nested
    class CreateToken {

        @Test
        void deletesExistingTokenThenSavesNewOneWithExpiryFromProperties() {
            when(properties.getRefreshExpiration()).thenReturn(604800000L);
            when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            RefreshToken result = refreshTokenService.createToken(userId);

            verify(repository, times(1)).deleteByUserId(userId);
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(repository).save(captor.capture());
            RefreshToken saved = captor.getValue();

            assertThat(saved.getUserId()).isEqualTo(userId);
            assertThat(saved.getToken()).isNotBlank();
            assertThat(saved.getRevoked()).isFalse();
            assertThat(saved.getExpiryDate()).isAfter(Instant.now().plusSeconds(604700));
            assertThat(result).isSameAs(saved);
        }

        @Test
        void generatesADifferentTokenValueOnEachCall() {
            when(properties.getRefreshExpiration()).thenReturn(604800000L);
            when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            RefreshToken first = refreshTokenService.createToken(userId);
            RefreshToken second = refreshTokenService.createToken(userId);

            assertThat(first.getToken()).isNotEqualTo(second.getToken());
        }
    }

    @Nested
    class ValidateToken {

        @Test
        void returnsTokenWhenValidAndNotExpired() {
            RefreshToken token = RefreshToken.builder().userId(userId).token("valid-token")
                .expiryDate(Instant.now().plusSeconds(3600)).revoked(false).build();
            when(repository.findByToken("valid-token")).thenReturn(Optional.of(token));

            RefreshToken result = refreshTokenService.validateToken("valid-token");

            assertThat(result).isEqualTo(token);
            verify(repository, never()).delete(any());
        }

        @Test
        void throwsBadCredentialsWhenTokenNotFound() {
            when(repository.findByToken("missing-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.validateToken("missing-token"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("not found");
        }

        @Test
        void throwsBadCredentialsWhenTokenRevoked() {
            RefreshToken token = RefreshToken.builder().userId(userId).token("revoked-token")
                .expiryDate(Instant.now().plusSeconds(3600)).revoked(true).build();
            when(repository.findByToken("revoked-token")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> refreshTokenService.validateToken("revoked-token"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("revoked");

            verify(repository, never()).delete(any());
        }

        @Test
        void deletesAndThrowsBadCredentialsWhenTokenExpired() {
            RefreshToken token = RefreshToken.builder().userId(userId).token("expired-token")
                .expiryDate(Instant.now().minusSeconds(1)).revoked(false).build();
            when(repository.findByToken("expired-token")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> refreshTokenService.validateToken("expired-token"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("expired");

            verify(repository).delete(token);
        }
    }

    @Test
    void deleteTokenDelegatesToRepository() {
        refreshTokenService.deleteToken(userId);

        verify(repository).deleteByUserId(userId);
    }
}
