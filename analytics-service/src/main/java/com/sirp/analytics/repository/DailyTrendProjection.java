package com.sirp.analytics.repository;

import java.sql.Date;

/**
 * Native-query projection for the daily trend aggregation - Spring Data
 * binds each column alias below to the matching getter.
 */
public interface DailyTrendProjection {

    Date getDay();

    long getCreatedCount();

    long getResolvedCount();

    long getClosedCount();
}
