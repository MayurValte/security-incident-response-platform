package com.sirp.auth.security;

import com.sirp.security.constants.JwtClaimConstants;
import com.sirp.security.jwt.JwtKeyProvider;
import com.sirp.security.properties.JwtProperties;
import io.jsonwebtoken.Jwts;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * RENAMED from "JwtService" to "TokenIssuerService" - deliberately, so
 * its name can never again collide with the shared library's
 * JwtValidationService (or anything else in com.sirp.security). This is
 * the ONLY class in the whole platform that calls
 * JwtKeyProvider.getPrivateKey() - i.e. the only class that signs tokens.
 *
 * ISSUING ONLY. Unlike your earlier version, this class no longer has
 * extractUsername()/validate()/extractClaims() - the Auth Service now
 * protects its OWN endpoints (e.g. /logout) with the exact same shared
 * JwtAuthenticationFilter + JwtValidationService that every downstream
 * service uses (see SecurityConfig). There is no longer a second,
 * parallel validation path living inside the Auth Service - one
 * implementation validates tokens everywhere, full stop.
 *
 * JwtKeyProvider and JwtProperties are autowired from the shared library
 * (provided automatically by SirpSecurityCoreAutoConfiguration).
 */
@Service
@RequiredArgsConstructor
public class TokenIssuerService {

    private final JwtProperties properties;
    private final JwtKeyProvider keyProvider;

    public String generateAccessToken(UUID userId, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimConstants.USER_ID, userId.toString());
        claims.put(JwtClaimConstants.ROLE, role);
        return buildToken(claims, email, properties.getExpiration());
    }

    private String buildToken(Map<String, Object> claims, String subject, Long expiration) {
        return Jwts.builder()
                   .claims(claims)
                   .subject(subject)
                   .issuer(JwtClaimConstants.ISSUER)
                   .audience()
                   .add(JwtClaimConstants.AUDIENCE)
                   .and()
                   .id(UUID.randomUUID().toString())
                   .issuedAt(new Date())
                   .expiration(new Date(System.currentTimeMillis() + expiration))
                   .signWith(keyProvider.getPrivateKey(), Jwts.SIG.RS256)
                   .compact();
    }
}

