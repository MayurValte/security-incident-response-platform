package com.sirp.auth.dto.response;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long expiresIn
) {
}
