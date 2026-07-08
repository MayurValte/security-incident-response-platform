package com.sirp.audit.audit.specification;

import com.sirp.audit.audit.entity.AggregateType;
import com.sirp.audit.audit.entity.AuditEvent;
import com.sirp.audit.audit.entity.AuditEventType;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class AuditSpecification {

  private AuditSpecification() {
  }

  public static Specification<AuditEvent> aggregateId(UUID aggregateId) {
    return (root, query, cb) ->
        aggregateId == null
            ? cb.conjunction()
            : cb.equal(root.get("aggregateId"), aggregateId);
  }

  public static Specification<AuditEvent> aggregateType(AggregateType type) {
    return (root, query, cb) ->
        type == null
            ? cb.conjunction()
            : cb.equal(root.get("aggregateType"), type);
  }

  public static Specification<AuditEvent> eventType(AuditEventType eventType) {
    return (root, query, cb) ->
        eventType == null
            ? cb.conjunction()
            : cb.equal(root.get("eventType"), eventType);
  }

  public static Specification<AuditEvent> performedBy(UUID userId) {
    return (root, query, cb) ->
        userId == null
            ? cb.conjunction()
            : cb.equal(root.get("performedBy"), userId);
  }

  public static Specification<AuditEvent> occurredAfter(Instant timestamp) {
    return (root, query, cb) ->
        timestamp == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("occurredAt"), timestamp);
  }

  public static Specification<AuditEvent> occurredBefore(Instant timestamp) {
    return (root, query, cb) ->
        timestamp == null
            ? cb.conjunction()
            : cb.lessThanOrEqualTo(root.get("occurredAt"), timestamp);
  }
}