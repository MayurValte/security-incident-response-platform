package com.sirp.common.events;

import java.time.Instant;
import java.util.UUID;

public record UserUpdatedEvent(
    UUID eventId,
    UUID userId,
    String username,
    String email,
    String role,
    UUID updatedBy,
    Instant occurredAt
) {
}
