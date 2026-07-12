package com.sirp.analytics.entity;

import com.sirp.common.enums.IncidentPriority;
import com.sirp.common.enums.IncidentSeverity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row per incident, built up incrementally from the four incident
 * lifecycle Kafka events (created/assigned/resolved/closed). This is a
 * read model, not an audit trail - unlike audit-service, it doesn't keep
 * every event, just the current derived state needed for aggregation
 * queries (counts by severity/priority, resolution time).
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "incident_analytics",
    indexes = {
        @Index(name = "idx_incident_analytics_incident_id", columnList = "incident_id"),
        @Index(name = "idx_incident_analytics_created_at", columnList = "created_at"),
        @Index(name = "idx_incident_analytics_severity", columnList = "severity"),
        @Index(name = "idx_incident_analytics_priority", columnList = "priority")
    }
)
public class IncidentAnalyticsRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "incident_id", nullable = false, unique = true)
    private UUID incidentId;

    @Column(name = "incident_number", length = 50)
    private String incidentNumber;

    @Column(name = "title", length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20)
    private IncidentPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AnalyticsStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "resolution_minutes")
    private Long resolutionMinutes;
}
