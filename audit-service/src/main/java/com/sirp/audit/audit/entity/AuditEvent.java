package com.sirp.audit.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "audit_event", indexes = {@Index(name = "idx_audit_event_type", columnList = "event_type"), @Index(name = "idx_audit_aggregate", columnList = "aggregate_id"), @Index(name = "idx_performed_by", columnList = "performed_by"), @Index(name = "idx_occurred_at", columnList = "occurred_at")})
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;
    @Enumerated(EnumType.STRING)
    @Column(name = "aggregate_type", nullable = false)
    private AggregateType aggregateType;
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private AuditEventType eventType;
    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;
    @Column(name = "performed_by")
    private UUID performedBy;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;
    @Version
    private Long version;
}