package com.sirp.common.events;

import java.time.Instant;
import java.util.UUID;

public record AuthLoginFailedEvent(
    UUID eventId,
    String email,
    String reason,
    Instant occurredAt
) {
}
