package com.sirp.incident.incident.dto.response;

import com.sirp.common.enums.IncidentPriority;
import com.sirp.common.enums.IncidentSeverity;
import com.sirp.incident.incident.enums.IncidentStatus;
import java.time.Instant;
import java.util.UUID;

public record IncidentSummaryResponse(

    UUID id,

    String incidentNumber,

    String title,

    IncidentStatus status,

    IncidentSeverity severity,

    IncidentPriority priority,

    UUID assignedTo,

    Instant createdAt

) {

}