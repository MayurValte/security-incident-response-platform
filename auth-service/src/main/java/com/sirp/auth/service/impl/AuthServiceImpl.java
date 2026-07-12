package com.sirp.auth.service.impl;

import com.sirp.auth.dto.request.LoginRequest;
import com.sirp.auth.dto.request.RefreshTokenRequest;
import com.sirp.auth.dto.response.AuthResponse;
import com.sirp.auth.dto.response.UserSecurityResponse;
import com.sirp.auth.entity.RefreshToken;
import com.sirp.auth.feign.ResilientUserClient;
import com.sirp.auth.security.TokenIssuerService;
import com.sirp.auth.service.AuthService;
import com.sirp.auth.service.RefreshTokenService;
import com.sirp.security.properties.JwtProperties;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MODIFIED: JwtService -> TokenIssuerService (renamed, issuing-only),
 * com.sirp.auth.security.JwtProperties -> shared
 * com.sirp.security.properties.JwtProperties. Logic is otherwise
 * identical to your uploaded version.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final ResilientUserClient userClient;
    private final TokenIssuerService tokenIssuerService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        UserSecurityResponse user = userClient.findByEmail(request.email());
        String accessToken = tokenIssuerService.generateAccessToken(user.id(), user.email(), user.role());
        RefreshToken refreshToken = refreshTokenService.createToken(user.id());
        return new AuthResponse(accessToken, refreshToken.getToken(), "Bearer",
                                jwtProperties.getExpiration());
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenService.validateToken(request.refreshToken());
        UserSecurityResponse user = userClient.findById(token.getUserId());
        String accessToken = tokenIssuerService.generateAccessToken(user.id(), user.email(), user.role());
        RefreshToken newRefreshToken = refreshTokenService.createToken(user.id());
        return new AuthResponse(accessToken, newRefreshToken.getToken(), "Bearer",
                                jwtProperties.getExpiration());
    }

    @Override
    public void logout(UUID userId) {
        refreshTokenService.deleteToken(userId);
    }
}
