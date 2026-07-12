package com.sirp.analytics.service.impl;

import com.sirp.analytics.dto.response.AnalyticsSummaryResponse;
import com.sirp.analytics.dto.response.DailyTrendResponse;
import com.sirp.analytics.dto.response.PriorityBreakdownResponse;
import com.sirp.analytics.dto.response.SeverityBreakdownResponse;
import com.sirp.analytics.repository.IncidentAnalyticsRepository;
import com.sirp.analytics.service.AnalyticsService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final IncidentAnalyticsRepository repository;

    @Override
    public AnalyticsSummaryResponse getSummary(Instant from, Instant to) {
        return repository.getSummary(from, to);
    }

    @Override
    public List<SeverityBreakdownResponse> getBySeverity(Instant from, Instant to) {
        return repository.getBySeverity(from, to);
    }

    @Override
    public List<PriorityBreakdownResponse> getByPriority(Instant from, Instant to) {
        return repository.getByPriority(from, to);
    }

    @Override
    public List<DailyTrendResponse> getDailyTrend(Instant from, Instant to) {
        return repository.getDailyTrend(from, to)
                         .stream()
                         .map(p -> new DailyTrendResponse(p.getDay().toLocalDate(), p.getCreatedCount(),
                                                          p.getResolvedCount(), p.getClosedCount()))
                         .toList();
    }
}
