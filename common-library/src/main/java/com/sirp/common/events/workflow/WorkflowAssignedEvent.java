package com.sirp.common.events.workflow;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkflowAssignedEvent(

    UUID eventId,

    UUID workflowId,

    UUID incidentId,

    UUID assignedTo,

    UUID assignedTeam,

    LocalDateTime assignedAt

) {

}