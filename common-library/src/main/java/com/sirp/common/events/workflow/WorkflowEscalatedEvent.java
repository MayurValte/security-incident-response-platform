package com.sirp.common.events.workflow;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkflowEscalatedEvent(

    UUID eventId,

    UUID workflowId,

    UUID incidentId,

    Integer escalationLevel,

    UUID assignedTo,

    UUID assignedTeam,

    LocalDateTime escalatedAt,

    String remarks

) {

}