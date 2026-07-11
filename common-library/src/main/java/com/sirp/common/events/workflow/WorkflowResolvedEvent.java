package com.sirp.common.events.workflow;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkflowResolvedEvent(

    UUID eventId,

    UUID workflowId,

    UUID incidentId,

    UUID resolvedBy,

    LocalDateTime resolvedAt,

    String resolutionSummary

) {

}