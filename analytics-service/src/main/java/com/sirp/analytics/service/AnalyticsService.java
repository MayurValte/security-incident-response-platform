package com.sirp.analytics.service;

import com.sirp.analytics.dto.response.AnalyticsSummaryResponse;
import com.sirp.analytics.dto.response.DailyTrendResponse;
import com.sirp.analytics.dto.response.PriorityBreakdownResponse;
import com.sirp.analytics.dto.response.SeverityBreakdownResponse;
import java.time.Instant;
import java.util.List;

public interface AnalyticsService {

    AnalyticsSummaryResponse getSummary(Instant from, Instant to);

    List<SeverityBreakdownResponse> getBySeverity(Instant from, Instant to);

    List<PriorityBreakdownResponse> getByPriority(Instant from, Instant to);

    List<DailyTrendResponse> getDailyTrend(Instant from, Instant to);
}
