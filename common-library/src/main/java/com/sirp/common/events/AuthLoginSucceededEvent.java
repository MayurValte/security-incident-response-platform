package com.sirp.common.events;

import java.time.Instant;
import java.util.UUID;

public record AuthLoginSucceededEvent(
    UUID eventId,
    UUID userId,
    String email,
    Instant occurredAt
) {
}
