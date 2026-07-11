package com.sirp.common.events;

import com.sirp.common.enums.IncidentPriority;
import com.sirp.common.enums.IncidentSeverity;
import java.time.Instant;
import java.util.UUID;

public record IncidentCreatedEvent(

    UUID eventId,

    UUID incidentId,

    String incidentNumber,

    String title,

    String description,

    IncidentPriority priority,

    IncidentSeverity severity,

    UUID createdBy,

    Instant occurredAt

) {

}