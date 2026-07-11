package com.sirp.auth.service.impl;

import com.sirp.auth.entity.RefreshToken;
import com.sirp.auth.repository.RefreshTokenRepository;
import com.sirp.auth.service.RefreshTokenService;
import com.sirp.security.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

// MODIFIED: import com.sirp.auth.security.JwtProperties -> shared com.sirp.security.properties.JwtProperties.
// validateToken() now throws BadCredentialsException (an AuthenticationException)
// instead of plain RuntimeException, so POST /api/v1/auth/refresh with a
// missing/revoked/expired token returns 401 via GlobalExceptionHandler's
// AuthenticationException handler instead of falling through to the
// generic 500 handler.
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final JwtProperties properties;

    @Override
    public RefreshToken createToken(UUID userId) {
        repository.deleteByUserId(userId);
        RefreshToken token = RefreshToken.builder().userId(userId).token(UUID.randomUUID().toString()).expiryDate(
                Instant.now().plusMillis(properties.getRefreshExpiration())).revoked(false).build();
        return repository.save(token);
    }

    @Override
    public RefreshToken validateToken(String token) {
        RefreshToken refreshToken = repository.findByToken(token).orElseThrow(
                () -> new BadCredentialsException("Refresh token not found"));
        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {
            throw new BadCredentialsException("Refresh token revoked");
        }
        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            repository.delete(refreshToken);
            throw new BadCredentialsException("Refresh token expired");
        }
        return refreshToken;
    }

    @Override
    public void deleteToken(UUID userId) {
        repository.deleteByUserId(userId);
    }
}
