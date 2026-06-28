package com.sirp.common.events;

import java.time.LocalDateTime;

public record IncidentAssignedEvent(

        Long incidentId,

        Long assigneeId,

        LocalDateTime assignedAt

) {
}
