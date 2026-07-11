package com.sirp.security.model;

import java.util.UUID;

/**
 * Stateless representation of the authenticated principal, built directly
 * from token claims - no database or user-service lookup involved.
 */
public record JwtUser(UUID userId, String email, String role, UUID teamId) {
}
