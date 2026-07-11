package com.sirp.incident.incident.dto.response;

import com.sirp.common.enums.IncidentPriority;
import com.sirp.common.enums.IncidentSeverity;
import com.sirp.incident.incident.enums.IncidentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IncidentResponse(

    UUID id,

    String incidentNumber,

    String title,

    String description,

    IncidentStatus status,

    IncidentSeverity severity,

    IncidentPriority priority,

    UUID createdBy,

    UUID assignedTo,

    UUID teamId,

    Instant createdAt,

    Instant updatedAt,

    Instant resolvedAt,

    Instant closedAt,

    List<CommentResponse> comments

) {

}