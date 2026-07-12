package com.sirp.analytics.dto.response;

public record AnalyticsSummaryResponse(

    long totalCreated,

    long totalAssigned,

    long totalResolved,

    long totalClosed,

    Double avgResolutionMinutes

) {

}
