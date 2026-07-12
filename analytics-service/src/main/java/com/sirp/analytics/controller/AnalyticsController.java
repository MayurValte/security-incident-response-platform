package com.sirp.analytics.controller;

import com.sirp.analytics.dto.response.AnalyticsSummaryResponse;
import com.sirp.analytics.dto.response.DailyTrendResponse;
import com.sirp.analytics.dto.response.PriorityBreakdownResponse;
import com.sirp.analytics.dto.response.SeverityBreakdownResponse;
import com.sirp.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics API", description = "Incident metrics and dashboard APIs")
public class AnalyticsController {

    private static final int DEFAULT_RANGE_DAYS = 30;

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    @Operation(summary = "Overall incident counts and average resolution time for a date range")
    public ResponseEntity<AnalyticsSummaryResponse> getSummary(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Instant[] range = resolveRange(from, to);
        return ResponseEntity.ok(analyticsService.getSummary(range[0], range[1]));
    }

    @GetMapping("/by-severity")
    @Operation(summary = "Incident counts and average resolution time grouped by severity")
    public ResponseEntity<List<SeverityBreakdownResponse>> getBySeverity(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Instant[] range = resolveRange(from, to);
        return ResponseEntity.ok(analyticsService.getBySeverity(range[0], range[1]));
    }

    @GetMapping("/by-priority")
    @Operation(summary = "Incident counts and average resolution time grouped by priority")
    public ResponseEntity<List<PriorityBreakdownResponse>> getByPriority(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Instant[] range = resolveRange(from, to);
        return ResponseEntity.ok(analyticsService.getByPriority(range[0], range[1]));
    }

    @GetMapping("/trend")
    @Operation(summary = "Daily created/resolved/closed counts for a date range")
    public ResponseEntity<List<DailyTrendResponse>> getDailyTrend(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Instant[] range = resolveRange(from, to);
        return ResponseEntity.ok(analyticsService.getDailyTrend(range[0], range[1]));
    }

    private Instant[] resolveRange(Instant from, Instant to) {
        Instant resolvedTo = to != null ? to : Instant.now();
        Instant resolvedFrom = from != null ? from : resolvedTo.minus(DEFAULT_RANGE_DAYS, ChronoUnit.DAYS);
        return new Instant[] {resolvedFrom, resolvedTo};
    }
}
