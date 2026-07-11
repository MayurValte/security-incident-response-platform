package com.sirp.common.events.workflow;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkflowClosedEvent(

    UUID eventId,

    UUID workflowId,

    UUID incidentId,

    LocalDateTime closedAt

) {

}