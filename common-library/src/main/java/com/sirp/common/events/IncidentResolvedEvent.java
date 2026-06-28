package com.sirp.common.events;

import java.time.LocalDateTime;

public record IncidentResolvedEvent(

        Long incidentId,

        LocalDateTime resolvedAt

) {
}