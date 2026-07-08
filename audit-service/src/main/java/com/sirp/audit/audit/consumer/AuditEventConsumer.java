package com.sirp.audit.audit.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirp.audit.audit.entity.AggregateType;
import com.sirp.audit.audit.entity.AuditEvent;
import com.sirp.audit.audit.entity.AuditEventType;
import com.sirp.audit.audit.repository.AuditEventRepository;
import com.sirp.common.events.IncidentAssignedEvent;
import com.sirp.common.events.IncidentClosedEvent;
import com.sirp.common.events.IncidentCreatedEvent;
import com.sirp.common.events.IncidentResolvedEvent;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventConsumer {

  private final AuditEventRepository repository;
  private final ObjectMapper objectMapper;

  @KafkaListener(topics = "incident.created.v1", groupId = "audit-service")
  public void consumeCreated(IncidentCreatedEvent event) {
    persist(
        event.eventId(),
        event.incidentId(),
        AuditEventType.INCIDENT_CREATED,
        event.createdBy(),
        event.occurredAt(),
        event,
        "incident-service");
  }

  @KafkaListener(topics = "incident.assigned.v1", groupId = "audit-service")
  public void consumeAssigned(IncidentAssignedEvent event) {
    persist(
        event.eventId(),
        event.incidentId(),
        AuditEventType.INCIDENT_ASSIGNED,
        event.assignedBy(),
        event.occurredAt(),
        event,
        "incident-service");
  }

  @KafkaListener(topics = "incident.resolved.v1", groupId = "audit-service")
  public void consumeResolved(IncidentResolvedEvent event) {
    persist(
        event.eventId(),
        event.incidentId(),
        AuditEventType.INCIDENT_RESOLVED,
        event.resolvedBy(),
        event.occurredAt(),
        event,
        "incident-service");
  }

  @KafkaListener(topics = "incident.closed.v1", groupId = "audit-service")
  public void consumeClosed(IncidentClosedEvent event) {
    persist(
        event.eventId(),
        event.incidentId(),
        AuditEventType.INCIDENT_CLOSED,
        event.closedBy(),
        event.occurredAt(),
        event,
        "incident-service");
  }

  private void persist(
      UUID eventId,
      UUID aggregateId,
      AuditEventType eventType,
      UUID performedBy,
      Instant occurredAt,
      Object payload,
      String serviceName) {

    try {
      if (repository.existsByEventId(eventId)) {
        log.debug("Duplicate audit event ignored [{}]", eventId);
        return;
      }

      AuditEvent event = AuditEvent.builder()
                                   .eventId(eventId)
                                   .aggregateId(aggregateId)
                                   .aggregateType(AggregateType.INCIDENT)
                                   .eventType(eventType)
                                   .performedBy(performedBy)
                                   .occurredAt(occurredAt)
                                   .serviceName(serviceName)
                                   .payload(objectMapper.writeValueAsString(payload))
                                   .build();

      repository.save(event);

      log.info("Persisted audit event [{}] aggregate [{}]", eventType, aggregateId);

    } catch (JsonProcessingException ex) {
      log.error("Failed to serialize payload", ex);
    }
  }
}
