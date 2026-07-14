package com.sirp.workflow.feign.dto;

import com.sirp.common.enums.IncidentPriority;
import com.sirp.common.enums.IncidentSeverity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IncidentResponse(

        UUID id,

        String incidentNumber,

        String title,

        String description,

        String status,

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