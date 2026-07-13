package com.sirp.common.events;

import java.time.Instant;
import java.util.UUID;

public record UserDeletedEvent(
    UUID eventId,
    UUID userId,
    UUID deletedBy,
    Instant occurredAt
) {
}
