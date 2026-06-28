package com.sirp.common.events;

import java.time.LocalDateTime;

public record IncidentCreatedEvent(

        Long incidentId,

        String title,

        String severity,

        Long createdBy,

        LocalDateTime createdAt

) {
}
