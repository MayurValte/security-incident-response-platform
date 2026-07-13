package com.sirp.audit.audit.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirp.audit.audit.entity.AggregateType;
import com.sirp.audit.audit.entity.AuditEvent;
import com.sirp.audit.audit.entity.AuditEventType;
import com.sirp.audit.audit.repository.AuditEventRepository;
import com.sirp.common.events.AuthLoginFailedEvent;
import com.sirp.common.events.AuthLoginSucceededEvent;
import com.sirp.common.events.IncidentAssignedEvent;
import com.sirp.common.events.IncidentClosedEvent;
import com.sirp.common.events.IncidentCreatedEvent;
import com.sirp.common.events.IncidentResolvedEvent;
import com.sirp.common.events.UserCreatedEvent;
import com.sirp.common.events.UserDeletedEvent;
import com.sirp.common.events.UserUpdatedEvent;
import com.sirp.common.events.workflow.WorkflowAssignedEvent;
import com.sirp.common.events.workflow.WorkflowClosedEvent;
import com.sirp.common.events.workflow.WorkflowCreatedEvent;
import com.sirp.common.events.workflow.WorkflowEscalatedEvent;
import com.sirp.common.events.workflow.WorkflowResolvedEvent;
import com.sirp.common.kafka.KafkaTopics;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

  @KafkaListener(topics = KafkaTopics.INCIDENT_CREATED, groupId = "audit-service")
  public void consumeCreated(IncidentCreatedEvent event) {
    persist(
        event.eventId(),
        event.incidentId(),
        AggregateType.INCIDENT,
        AuditEventType.INCIDENT_CREATED,
        event.createdBy(),
        event.occurredAt(),
        event,
        "incident-service");
  }

  @KafkaListener(topics = KafkaTopics.INCIDENT_ASSIGNED, groupId = "audit-service")
  public void consumeAssigned(IncidentAssignedEvent event) {
    persist(
        event.eventId(),
        event.incidentId(),
        AggregateType.INCIDENT,
        AuditEventType.INCIDENT_ASSIGNED,
        event.assignedBy(),
        event.occurredAt(),
        event,
        "incident-service");
  }

  @KafkaListener(topics = KafkaTopics.INCIDENT_RESOLVED, groupId = "audit-service")
  public void consumeResolved(IncidentResolvedEvent event) {
    persist(
        event.eventId(),
        event.incidentId(),
        AggregateType.INCIDENT,
        AuditEventType.INCIDENT_RESOLVED,
        event.resolvedBy(),
        event.occurredAt(),
        event,
        "incident-service");
  }

  @KafkaListener(topics = KafkaTopics.INCIDENT_CLOSED, groupId = "audit-service")
  public void consumeClosed(IncidentClosedEvent event) {
    persist(
        event.eventId(),
        event.incidentId(),
        AggregateType.INCIDENT,
        AuditEventType.INCIDENT_CLOSED,
        event.closedBy(),
        event.occurredAt(),
        event,
        "incident-service");
  }

  @KafkaListener(topics = KafkaTopics.WORKFLOW_CREATED, groupId = "audit-service")
  public void consumeWorkflowCreated(WorkflowCreatedEvent event) {
    persist(
        event.eventId(),
        event.workflowId(),
        AggregateType.WORKFLOW,
        AuditEventType.WORKFLOW_CREATED,
        event.createdBy(),
        toInstant(event.createdAt()),
        event,
        "workflow-service");
  }

  @KafkaListener(topics = KafkaTopics.WORKFLOW_ASSIGNED, groupId = "audit-service")
  public void consumeWorkflowAssigned(WorkflowAssignedEvent event) {
    persist(
        event.eventId(),
        event.workflowId(),
        AggregateType.WORKFLOW,
        AuditEventType.WORKFLOW_ASSIGNED,
        event.assignedBy(),
        toInstant(event.assignedAt()),
        event,
        "workflow-service");
  }

  @KafkaListener(topics = KafkaTopics.WORKFLOW_ESCALATED, groupId = "audit-service")
  public void consumeWorkflowEscalated(WorkflowEscalatedEvent event) {
    persist(
        event.eventId(),
        event.workflowId(),
        AggregateType.WORKFLOW,
        AuditEventType.WORKFLOW_ESCALATED,
        event.escalatedBy(),
        toInstant(event.escalatedAt()),
        event,
        "workflow-service");
  }

  @KafkaListener(topics = KafkaTopics.WORKFLOW_RESOLVED, groupId = "audit-service")
  public void consumeWorkflowResolved(WorkflowResolvedEvent event) {
    persist(
        event.eventId(),
        event.workflowId(),
        AggregateType.WORKFLOW,
        AuditEventType.WORKFLOW_RESOLVED,
        event.resolvedBy(),
        toInstant(event.resolvedAt()),
        event,
        "workflow-service");
  }

  @KafkaListener(topics = KafkaTopics.WORKFLOW_CLOSED, groupId = "audit-service")
  public void consumeWorkflowClosed(WorkflowClosedEvent event) {
    persist(
        event.eventId(),
        event.workflowId(),
        AggregateType.WORKFLOW,
        AuditEventType.WORKFLOW_CLOSED,
        event.closedBy(),
        toInstant(event.closedAt()),
        event,
        "workflow-service");
  }

  @KafkaListener(topics = KafkaTopics.AUTH_LOGIN_SUCCEEDED, groupId = "audit-service")
  public void consumeLoginSucceeded(AuthLoginSucceededEvent event) {
    persist(
        event.eventId(),
        event.userId(),
        AggregateType.AUTH,
        AuditEventType.LOGIN_SUCCESS,
        event.userId(),
        event.occurredAt(),
        event,
        "auth-service");
  }

  @KafkaListener(topics = KafkaTopics.AUTH_LOGIN_FAILED, groupId = "audit-service")
  public void consumeLoginFailed(AuthLoginFailedEvent event) {
    // No real user id exists for a failed login against an unknown email -
    // a deterministic pseudo-id derived from the email lets repeated failed
    // attempts against the same address still correlate together in queries,
    // while aggregate_id stays non-null as the schema requires.
    UUID pseudoAggregateId = UUID.nameUUIDFromBytes(event.email().getBytes(StandardCharsets.UTF_8));
    persist(
        event.eventId(),
        pseudoAggregateId,
        AggregateType.AUTH,
        AuditEventType.LOGIN_FAILED,
        null,
        event.occurredAt(),
        event,
        "auth-service");
  }

  @KafkaListener(topics = KafkaTopics.USER_CREATED, groupId = "audit-service")
  public void consumeUserCreated(UserCreatedEvent event) {
    persist(
        event.eventId(),
        event.userId(),
        AggregateType.USER,
        AuditEventType.USER_CREATED,
        event.createdBy(),
        event.occurredAt(),
        event,
        "user-service");
  }

  @KafkaListener(topics = KafkaTopics.USER_UPDATED, groupId = "audit-service")
  public void consumeUserUpdated(UserUpdatedEvent event) {
    persist(
        event.eventId(),
        event.userId(),
        AggregateType.USER,
        AuditEventType.USER_UPDATED,
        event.updatedBy(),
        event.occurredAt(),
        event,
        "user-service");
  }

  @KafkaListener(topics = KafkaTopics.USER_DELETED, groupId = "audit-service")
  public void consumeUserDeleted(UserDeletedEvent event) {
    persist(
        event.eventId(),
        event.userId(),
        AggregateType.USER,
        AuditEventType.USER_DELETED,
        event.deletedBy(),
        event.occurredAt(),
        event,
        "user-service");
  }

  private void persist(
      UUID eventId,
      UUID aggregateId,
      AggregateType aggregateType,
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
                                   .aggregateType(aggregateType)
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

  private Instant toInstant(LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(ZoneOffset.UTC);
  }
}
