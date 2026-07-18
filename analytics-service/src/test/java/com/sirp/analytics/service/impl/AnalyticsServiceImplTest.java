package com.sirp.analytics.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sirp.analytics.dto.response.AnalyticsSummaryResponse;
import com.sirp.analytics.dto.response.DailyTrendResponse;
import com.sirp.analytics.dto.response.PriorityBreakdownResponse;
import com.sirp.analytics.dto.response.SeverityBreakdownResponse;
import com.sirp.analytics.repository.DailyTrendProjection;
import com.sirp.analytics.repository.IncidentAnalyticsRepository;
import com.sirp.common.enums.IncidentPriority;
import com.sirp.common.enums.IncidentSeverity;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private IncidentAnalyticsRepository repository;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @Nested
    class RangeResolution {

        @Test
        void defaultsToTrailing30DaysWhenBothNull() {
            AnalyticsSummaryResponse response = new AnalyticsSummaryResponse(1, 1, 1, 1, 10.0);
            when(repository.getSummary(any(), any())).thenReturn(response);

            analyticsService.getSummary(null, null);

            ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
            ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(repository).getSummary(fromCaptor.capture(), toCaptor.capture());
            Instant to = toCaptor.getValue();
            Instant from = fromCaptor.getValue();
            assertThat(to).isCloseTo(Instant.now(), org.assertj.core.api.Assertions.within(5, ChronoUnit.SECONDS));
            assertThat(from).isEqualTo(to.minus(30, ChronoUnit.DAYS));
        }

        @Test
        void usesToAsAnchorWhenOnlyFromIsNull() {
            AnalyticsSummaryResponse response = new AnalyticsSummaryResponse(1, 1, 1, 1, 10.0);
            Instant to = Instant.parse("2026-07-01T00:00:00Z");
            when(repository.getSummary(any(), eq(to))).thenReturn(response);

            analyticsService.getSummary(null, to);

            verify(repository).getSummary(eq(to.minus(30, ChronoUnit.DAYS)), eq(to));
        }

        @Test
        void passesExplicitFromAndToThroughUnchanged() {
            Instant from = Instant.parse("2026-06-01T00:00:00Z");
            Instant to = Instant.parse("2026-07-01T00:00:00Z");
            when(repository.getSummary(from, to)).thenReturn(new AnalyticsSummaryResponse(1, 1, 1, 1, 10.0));

            analyticsService.getSummary(from, to);

            verify(repository).getSummary(from, to);
        }
    }

    @Test
    void getSummaryReturnsRepositoryResultDirectly() {
        Instant from = Instant.parse("2026-06-01T00:00:00Z");
        Instant to = Instant.parse("2026-07-01T00:00:00Z");
        AnalyticsSummaryResponse response = new AnalyticsSummaryResponse(5, 4, 3, 2, 42.5);
        when(repository.getSummary(from, to)).thenReturn(response);

        assertThat(analyticsService.getSummary(from, to)).isEqualTo(response);
    }

    @Test
    void getBySeverityDelegatesToRepositoryWithResolvedRange() {
        Instant from = Instant.parse("2026-06-01T00:00:00Z");
        Instant to = Instant.parse("2026-07-01T00:00:00Z");
        List<SeverityBreakdownResponse> response = List.of(
            new SeverityBreakdownResponse(IncidentSeverity.HIGH, 3, 12.0));
        when(repository.getBySeverity(from, to)).thenReturn(response);

        assertThat(analyticsService.getBySeverity(from, to)).isEqualTo(response);
    }

    @Test
    void getByPriorityDelegatesToRepositoryWithResolvedRange() {
        Instant from = Instant.parse("2026-06-01T00:00:00Z");
        Instant to = Instant.parse("2026-07-01T00:00:00Z");
        List<PriorityBreakdownResponse> response = List.of(
            new PriorityBreakdownResponse(IncidentPriority.P1, 3, 12.0));
        when(repository.getByPriority(from, to)).thenReturn(response);

        assertThat(analyticsService.getByPriority(from, to)).isEqualTo(response);
    }

    @Test
    void getDailyTrendMapsProjectionRowsToResponseConvertingSqlDateToLocalDate() {
        Instant from = Instant.parse("2026-06-01T00:00:00Z");
        Instant to = Instant.parse("2026-07-01T00:00:00Z");
        DailyTrendProjection projection = mockProjection(Date.valueOf("2026-06-15"), 3, 2, 1);
        when(repository.getDailyTrend(from, to)).thenReturn(List.of(projection));

        List<DailyTrendResponse> result = analyticsService.getDailyTrend(from, to);

        assertThat(result).containsExactly(
            new DailyTrendResponse(LocalDate.of(2026, 6, 15), 3, 2, 1));
    }

    private DailyTrendProjection mockProjection(Date day, long created, long resolved, long closed) {
        DailyTrendProjection projection = org.mockito.Mockito.mock(DailyTrendProjection.class);
        when(projection.getDay()).thenReturn(day);
        when(projection.getCreatedCount()).thenReturn(created);
        when(projection.getResolvedCount()).thenReturn(resolved);
        when(projection.getClosedCount()).thenReturn(closed);
        return projection;
    }
}
