package com.sirp.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

/**
 * MODIFIED for RS256: verifies signatures with the RSA public key from
 * JwtKeyProvider instead of an HMAC shared secret. No consuming service
 * needs any code changes to pick this up - JwtValidationService's public
 * API is unchanged.
 */
public class JwtTokenParser {

    private final JwtKeyProvider keyProvider;

    public JwtTokenParser(JwtKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    public Claims parse(String token) {
        return Jwts.parser()
            .verifyWith(keyProvider.getPublicKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
