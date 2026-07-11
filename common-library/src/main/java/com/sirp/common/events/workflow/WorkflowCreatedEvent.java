package com.sirp.common.events.workflow;

import com.sirp.common.enums.IncidentSeverity;
import java.time.LocalDateTime;
import java.util.UUID;

public record WorkflowCreatedEvent(

    UUID eventId,

    UUID workflowId,

    UUID incidentId,

    UUID assignedTo,

    UUID assignedTeam,

    IncidentSeverity severity,

    Integer escalationLevel,

    LocalDateTime slaDeadline,

    LocalDateTime nextEscalationTime,

    LocalDateTime createdAt

) {

}