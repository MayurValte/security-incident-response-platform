package com.sirp.auth.service;

import com.sirp.auth.entity.RefreshToken;
import java.util.UUID;

public interface RefreshTokenService {

    RefreshToken createToken(UUID userId);

    RefreshToken validateToken(String token);

    void deleteToken(UUID userId);
}
