package com.sirp.auth.dto.response;

import java.util.UUID;

/**
 * Shape returned by user-service's internal lookup endpoints (see
 * UserClient). Adjust field names to match your actual user-service
 * contract if it differs - this is the one place that needs to line up
 * exactly with what user-service serializes.
 */
public record UserSecurityResponse(
    UUID id,
    String email,
    String password,
    String role,
    Boolean enabled
) {
}
