package com.sirp.analytics.service.impl;

import com.sirp.analytics.dto.response.AnalyticsSummaryResponse;
import com.sirp.analytics.dto.response.DailyTrendResponse;
import com.sirp.analytics.dto.response.PriorityBreakdownResponse;
import com.sirp.analytics.dto.response.SeverityBreakdownResponse;
import com.sirp.analytics.repository.IncidentAnalyticsRepository;
import com.sirp.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final int DEFAULT_RANGE_DAYS = 30;

    private final IncidentAnalyticsRepository repository;

    @Override
    @Cacheable(cacheNames = "analytics-summary", key = "T(java.util.Objects).toString(#from) + '|' + T(java.util.Objects).toString(#to)")
    public AnalyticsSummaryResponse getSummary(Instant from, Instant to) {
        Instant[] range = resolveRange(from, to);
        return repository.getSummary(range[0], range[1]);
    }

    @Override
    @Cacheable(cacheNames = "analytics-by-severity", key = "T(java.util.Objects).toString(#from) + '|' + T(java.util.Objects).toString(#to)")
    public List<SeverityBreakdownResponse> getBySeverity(Instant from, Instant to) {
        Instant[] range = resolveRange(from, to);
        return repository.getBySeverity(range[0], range[1]);
    }

    @Override
    @Cacheable(cacheNames = "analytics-by-priority", key = "T(java.util.Objects).toString(#from) + '|' + T(java.util.Objects).toString(#to)")
    public List<PriorityBreakdownResponse> getByPriority(Instant from, Instant to) {
        Instant[] range = resolveRange(from, to);
        return repository.getByPriority(range[0], range[1]);
    }

    @Override
    @Cacheable(cacheNames = "analytics-trend", key = "T(java.util.Objects).toString(#from) + '|' + T(java.util.Objects).toString(#to)")
    public List<DailyTrendResponse> getDailyTrend(Instant from, Instant to) {
        Instant[] range = resolveRange(from, to);
        return repository.getDailyTrend(range[0], range[1])
                .stream()
                .map(p -> new DailyTrendResponse(p.getDay().toLocalDate(), p.getCreatedCount(),
                        p.getResolvedCount(), p.getClosedCount()))
                .toList();
    }

    private Instant[] resolveRange(Instant from, Instant to) {
        Instant resolvedTo = to != null ? to : Instant.now();
        Instant resolvedFrom = from != null ? from : resolvedTo.minus(DEFAULT_RANGE_DAYS, ChronoUnit.DAYS);
        return new Instant[]{resolvedFrom, resolvedTo};
    }
}
