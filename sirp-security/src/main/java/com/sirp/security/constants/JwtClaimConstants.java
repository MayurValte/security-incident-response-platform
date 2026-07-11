package com.sirp.security.constants;

/**
 * Single source of truth for JWT claim keys, issuer and audience values.
 * Referenced by every service - never hardcode these strings elsewhere.
 */
public final class JwtClaimConstants {

    private JwtClaimConstants() {
    }

    public static final String USER_ID = "userId";
    public static final String TEAM_ID = "teamId";
    public static final String ROLE = "role";

    public static final String ISSUER = "sirp-auth-service";
    public static final String AUDIENCE = "sirp-platform";
}
