package com.sirp.analytics.dto.response;

import com.sirp.common.enums.IncidentPriority;

public record PriorityBreakdownResponse(

    IncidentPriority priority,

    long incidentCount,

    Double avgResolutionMinutes

) {

}
