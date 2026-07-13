package com.sirp.common.events;

import java.time.Instant;
import java.util.UUID;

public record UserCreatedEvent(
    UUID eventId,
    UUID userId,
    String username,
    String email,
    String role,
    UUID createdBy,
    Instant occurredAt
) {
}
