package com.sirp.analytics.dto.response;

import com.sirp.common.enums.IncidentSeverity;

public record SeverityBreakdownResponse(

    IncidentSeverity severity,

    long incidentCount,

    Double avgResolutionMinutes

) {

}
