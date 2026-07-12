package com.sirp.analytics.dto.response;

import java.time.LocalDate;

public record DailyTrendResponse(

    LocalDate date,

    long createdCount,

    long resolvedCount,

    long closedCount

) {

}
