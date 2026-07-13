package com.sirp.incident.integration.user.dto;

import java.util.UUID;

/**
 * Mirrors the fields user-service's internal endpoint actually returns
 * (com.sirp.user.dto.UserSecurityResponse) - id/username/email/role/enabled.
 * There is no active/teamId/roles field on that response; this record
 * previously declared those names instead, which meant Feign's decoder
 * had nothing to bind them from.
 */
public record UserResponse(

        UUID id,

        String username,

        String email,

        String role,

        Boolean enabled

) {
}