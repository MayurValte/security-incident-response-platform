package com.sirp.analytics.repository;

import com.sirp.analytics.dto.response.AnalyticsSummaryResponse;
import com.sirp.analytics.dto.response.PriorityBreakdownResponse;
import com.sirp.analytics.dto.response.SeverityBreakdownResponse;
import com.sirp.analytics.entity.IncidentAnalyticsRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentAnalyticsRepository extends JpaRepository<IncidentAnalyticsRecord, UUID> {

    boolean existsByIncidentId(UUID incidentId);

    Optional<IncidentAnalyticsRecord> findByIncidentId(UUID incidentId);

    @Query("""
        select new com.sirp.analytics.dto.response.AnalyticsSummaryResponse(
            count(r),
            sum(case when r.assignedAt is not null then 1L else 0L end),
            sum(case when r.resolvedAt is not null then 1L else 0L end),
            sum(case when r.closedAt is not null then 1L else 0L end),
            avg(r.resolutionMinutes)
        )
        from IncidentAnalyticsRecord r
        where r.createdAt between :from and :to
        """)
    AnalyticsSummaryResponse getSummary(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        select new com.sirp.analytics.dto.response.SeverityBreakdownResponse(
            r.severity,
            count(r),
            avg(r.resolutionMinutes)
        )
        from IncidentAnalyticsRecord r
        where r.createdAt between :from and :to
        group by r.severity
        order by r.severity
        """)
    List<SeverityBreakdownResponse> getBySeverity(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        select new com.sirp.analytics.dto.response.PriorityBreakdownResponse(
            r.priority,
            count(r),
            avg(r.resolutionMinutes)
        )
        from IncidentAnalyticsRecord r
        where r.createdAt between :from and :to
        group by r.priority
        order by r.priority
        """)
    List<PriorityBreakdownResponse> getByPriority(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
        select day,
               coalesce(sum(created), 0) as createdCount,
               coalesce(sum(resolved), 0) as resolvedCount,
               coalesce(sum(closed), 0) as closedCount
        from (
            select cast(created_at as date) as day, 1 as created, 0 as resolved, 0 as closed
            from incident_analytics where created_at between :from and :to
            union all
            select cast(resolved_at as date) as day, 0 as created, 1 as resolved, 0 as closed
            from incident_analytics where resolved_at between :from and :to
            union all
            select cast(closed_at as date) as day, 0 as created, 0 as resolved, 1 as closed
            from incident_analytics where closed_at between :from and :to
        ) combined
        group by day
        order by day
        """, nativeQuery = true)
    List<DailyTrendProjection> getDailyTrend(@Param("from") Instant from, @Param("to") Instant to);
}
