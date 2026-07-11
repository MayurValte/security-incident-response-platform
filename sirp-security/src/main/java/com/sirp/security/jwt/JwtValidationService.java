package com.sirp.security.jwt;

import com.sirp.security.constants.JwtClaimConstants;
import com.sirp.security.model.JwtUser;
import io.jsonwebtoken.Claims;
import java.util.UUID;

/**
 * RENAMED from the earlier "JwtService" to "JwtValidationService".
 *
 * This was the exact class that collided with the Auth Service's own
 * (differently-purposed) JwtService during your last two restarts. Since
 * this shared class is read-only (verifies tokens, never signs them) and
 * the Auth Service's is a token issuer, they were always semantically
 * different things that happened to share a name. Renaming this one
 * removes the possibility of that collision permanently, for this class
 * and any future service that also wants to name its own issuing class
 * "JwtService".
 */
public class JwtValidationService {

    private final JwtTokenValidator validator;

    public JwtValidationService(JwtTokenValidator validator) {
        this.validator = validator;
    }

    public Claims getClaims(String token) {
        return validator.validate(token);
    }

    public JwtUser getUser(String token) {
        Claims claims = getClaims(token);
        String userId = claims.get(JwtClaimConstants.USER_ID, String.class);
        String teamId = claims.get(JwtClaimConstants.TEAM_ID, String.class);
        return new JwtUser(userId == null ? null : UUID.fromString(userId), claims.getSubject(),
                           claims.get(JwtClaimConstants.ROLE, String.class),
                           teamId == null ? null : UUID.fromString(teamId));
    }

    public String getSubject(String token) {
        return getClaims(token).getSubject();
    }
}
