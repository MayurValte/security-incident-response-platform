package com.sirp.security.jwt;

import com.sirp.security.constants.JwtClaimConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.Date;

public class JwtTokenValidator {

    private final JwtTokenParser jwtTokenParser;

    public JwtTokenValidator(JwtTokenParser jwtTokenParser) {
        this.jwtTokenParser = jwtTokenParser;
    }

    public Claims validate(String token) {
        Claims claims = jwtTokenParser.parse(token);

        if (claims.getExpiration() == null) {
            throw new JwtException("Token expiration is missing.");
        }
        if (claims.getExpiration().before(new Date())) {
            throw new JwtException("JWT token has expired.");
        }
        if (claims.getIssuer() == null || !JwtClaimConstants.ISSUER.equals(claims.getIssuer())) {
            throw new JwtException("JWT token issuer is invalid.");
        }
        if (claims.getAudience() == null || !claims.getAudience().contains(JwtClaimConstants.AUDIENCE)) {
            throw new JwtException("JWT token audience is invalid.");
        }
        return claims;
    }
}
