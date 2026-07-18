package com.sirp.audit.audit.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirp.audit.audit.entity.AggregateType;
import com.sirp.audit.audit.entity.AuditEvent;
import com.sirp.audit.audit.entity.AuditEventType;
import com.sirp.audit.audit.repository.AuditEventRepository;
import com.sirp.common.enums.IncidentPriority;
import com.sirp.common.enums.IncidentSeverity;
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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditEventConsumerTest {

    @Mock
    private AuditEventRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuditEventConsumer consumer;

    @Nested
    class IncidentEvents {

        @Test
        void consumeCreatedPersistsWithIncidentAggregateType() throws JsonProcessingException {
            UUID incidentId = UUID.randomUUID();
            UUID createdBy = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            IncidentCreatedEvent event = new IncidentCreatedEvent(eventId, incidentId, "INC-2026-1", "t", "d",
                IncidentPriority.P1, IncidentSeverity.HIGH, createdBy, Instant.now());
            when(repository.existsByEventId(eventId)).thenReturn(false);
            when(objectMapper.writeValueAsString(event)).thenReturn("{}");

            consumer.consumeCreated(event);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(repository).save(captor.capture());
            AuditEvent saved = captor.getValue();
            assertThat(saved.getEventId()).isEqualTo(eventId);
            assertThat(saved.getAggregateId()).isEqualTo(incidentId);
            assertThat(saved.getAggregateType()).isEqualTo(AggregateType.INCIDENT);
            assertThat(saved.getEventType()).isEqualTo(AuditEventType.INCIDENT_CREATED);
            assertThat(saved.getPerformedBy()).isEqualTo(createdBy);
            assertThat(saved.getServiceName()).isEqualTo("incident-service");
            assertThat(saved.getPayload()).isEqualTo("{}");
        }

        @Test
        void consumeAssignedUsesAssignedByAsPerformer() throws JsonProcessingException {
            UUID incidentId = UUID.randomUUID();
            UUID assignedBy = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            IncidentAssignedEvent event = new IncidentAssignedEvent(eventId, incidentId, "INC-2026-1", "t",
                IncidentPriority.P1, IncidentSeverity.HIGH, UUID.randomUUID(), assignedBy, Instant.now());
            when(objectMapper.writeValueAsString(event)).thenReturn("{}");

            consumer.consumeAssigned(event);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getEventType()).isEqualTo(AuditEventType.INCIDENT_ASSIGNED);
            assertThat(captor.getValue().getPerformedBy()).isEqualTo(assignedBy);
        }

        @Test
        void consumeResolvedUsesResolvedByAsPerformer() throws JsonProcessingException {
            UUID incidentId = UUID.randomUUID();
            UUID resolvedBy = UUID.randomUUID();
            IncidentResolvedEvent event = new IncidentResolvedEvent(UUID.randomUUID(), incidentId, "INC-2026-1",
                "t", IncidentPriority.P1, IncidentSeverity.HIGH, resolvedBy, Instant.now());
            when(objectMapper.writeValueAsString(event)).thenReturn("{}");

            consumer.consumeResolved(event);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getEventType()).isEqualTo(AuditEventType.INCIDENT_RESOLVED);
            assertThat(captor.getValue().getPerformedBy()).isEqualTo(resolvedBy);
        }

        @Test
        void consumeClosedUsesClosedByAsPerformer() throws JsonProcessingException {
            UUID incidentId = UUID.randomUUID();
            UUID closedBy = UUID.randomUUID();
            IncidentClosedEvent event = new IncidentClosedEvent(UUID.randomUUID(), incidentId, "INC-2026-1", "t",
                IncidentPriority.P1, IncidentSeverity.HIGH, closedBy, Instant.now());
            when(objectMapper.writeValueAsString(event)).thenReturn("{}");

            consumer.consumeClosed(event);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getEventType()).isEqualTo(AuditEventType.INCIDENT_CLOSED);
            assertThat(captor.getValue().getPerformedBy()).isEqualTo(closedBy);
        }
    }

    @Nested
    class WorkflowEvents {

        @Test
        void consumeWorkflowCreatedConvertsLocalDateTimeToInstant() throws JsonProcessingException {
            UUID workflowId = UUID.randomUUID();
            UUID createdBy = UUID.randomUUID();
            LocalDateTime createdAt = LocalDateTime.of(2026, 7, 18, 10, 30);
            WorkflowCreatedEvent event = new WorkflowCreatedEvent(UUID.randomUUID(), workflowId,
                UUID.randomUUID(), null, null, createdBy, IncidentSeverity.HIGH, 0, null, null, createdAt);
            when(objectMapper.writeValueAsString(event)).thenReturn("{}");

            consumer.consumeWorkflowCreated(event);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(repository).save(captor.capture());
            AuditEvent saved = captor.getValue();
            assertThat(saved.getAggregateId()).isEqualTo(workflowId);
            assertThat(saved.getAggregateType()).isEqualTo(AggregateType.WORKFLOW);
            assertThat(saved.getEventType()).isEqualTo(AuditEventType.WORKFLOW_CREATED);
            assertThat(saved.getServiceName()).isEqualTo("workflow-service");
            assertThat(saved.getOccurredAt()).isEqualTo(createdAt.toInstant(ZoneOffset.UTC));
        }

        @Test
        void consumeWorkflowCreatedToleratesNullCreatedAt() throws JsonProcessingException {
            WorkflowCreatedEvent event = new WorkflowCreatedEvent(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), null, null, UUID.randomUUID(), IncidentSeverity.HIGH, 0, null, null, null);
            when(objectMapper.writeValueAsString(event)).thenReturn("{}");

            consumer.consumeWorkflowCreated(event);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getOccurredAt()).isNull();
        }

        @Test
        void consumeWorkflowAssignedUsesAssignedBy() throws JsonProcessingException {
            UUID assignedBy = UUID.randomUUID();
            WorkflowAssignedEvent event = new WorkflowAssignedEvent(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), null, assignedBy, LocalDateTime.now());
            when(objectMapper.writeValueAsString(event)).thenReturn("{}");

            consumer.consumeWorkflowAssigned(event);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getEventType()).isEqualTo(AuditEventType.WORKFLOW_ASSIGNED);
            assertThat(captor.getValue().getPerformedBy()).isEqualTo(assignedBy);
        }

        @Test
        void consumeWorkflowEscalatedUsesEscalatedByAndToleratesNullActor() throws JsonProcessingException {
            WorkflowEscalatedEvent event = new WorkflowEscalatedEvent(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), 2, UUID.randomUUID(), null, null, LocalDateTime.now(), "auto-escalated");
            when(objectMapper.writeValueAsString(event)).thenReturn("{}");

            consumer.consumeWorkflowEscalated(event);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getEventType()).isEqualTo(AuditEventType.WORKFLOW_ESCALATED);
            assertThat(captor.getValue().getPerformedBy()).isNull();
        }

        @Test
        void consumeWorkflowResolvedUsesResolvedBy() throws JsonProcessingException {
            UUID resolvedBy = UUID.randomUUID();
            WorkflowResolvedEvent event = new WorkflowResolvedEvent(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), resolvedBy, LocalDateTime.now(), "fixed");
            when(objectMapper.writeValueAsString(event)).thenReturn("{}");

            consumer.consumeWorkflowResolved(event);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getEventType()).isEqualTo(AuditEventType.WORKFLOW_RESOLVED);
            assertThat(captor.getValue().getPerformedBy()).isEqualTo(resolvedBy);
        }

        @Test
        void consumeWorkflowClosedUsesClosedBy() throws JsonProcessingException {
            UUID closedBy = UUID.randomUUID();
            WorkflowClosedEvent event = new WorkflowClosedEvent(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), closedBy, LocalDateTime.now());
            when(objectMapper.writeValueAsString(event)).thenReturn("{}");

            consumer.consumeWorkflowClosed(event);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getEventType()).isEqualTo(AuditEventType.WORKFLOW_CLOSED);
            assertThat(captor.getValue().getPerformedBy()).isEqualTo(closedBy);
        }
    }

    @Nested
    class AuthEvents {

        @Test
        void consumeLoginSucceededUsesRealUserIdAsAggregate() throws JsonProcessingException {
            UUID userId = UUID.randomUUID();
            AuthLoginSucceededEvent event = new AuthLoginSucceededEvent(UUID.randomUUID(), userId,
                "jdoe@sirp.local", Instant.now());
            when(objectMapper.writeValueAsString(event)).thenReturn("{}");

            consumer.consumeLoginSucceeded(event);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getAggregateId()).isEqualTo(userId);
            assertThat(captor.getValue().getAggregateType()).isEqualTo(AggregateType.AUTH);
            assertThat(captor.getValue().getEventType()).isEqualTo(AuditEventType.LOGIN_SUCCESS);
            assertThat(captor.getValue().getPerformedBy()).isEqualTo(userId);
        }

        @Test
        void consumeLoginFailedDerivesDeterministicPseudoAggregateIdFromEmail() throws JsonProcessingException {
            String email = "unknown@sirp.local";
            AuthLoginFailedEvent event = new AuthLoginFailedEvent(UUID.randomUUID(), email,
                "BadCredentialsException", Instant.now());
            when(objectMapper.writeValueAsString(event)).thenReturn("{}");
            UUID expectedPseudoId = UUID.nameUUIDFromBytes(email.getBytes(StandardCharsets.UTF_8));

            consumer.consumeLoginFailed(event);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getAggregateId()).isEqualTo(expectedPseudoId);
            assertThat(captor.getValue().getEventType()).isEqualTo(AuditEventType.LOGIN_FAILED);
            assertThat(captor.getValue().getPerformedBy()).isNull();
        }

        @Test
        void consumeLoginFailedProducesTheSamePseudoIdForRepeatedAttemptsAgainstSameEmail()
            throws JsonProcessingException {
            String email = "unknown@sirp.local";
            AuthLoginFailedEvent first = new AuthLoginFailedEvent(UUID.randomUUID(), email, "reason1",
                Instant.now());
            AuthLoginFailedEvent second = new AuthLoginFailedEvent(UUID.randomUUID(), email, "reason2",
                Instant.now());
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");

            consumer.consumeLoginFailed(first);
            consumer.consumeLoginFailed(second);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(repository, org.mockito.Mockito.times(2)).save(captor.capture());
            assertThat(captor.getAllValues().get(0).getAggregateId())
                .isEqualTo(captor.getAllValues().get(1).getAggregateId());
        }
    }

    @Nested
    class UserEvents {

        @Test
        void consumeUserCreatedUsesCreatedBy() throws JsonProcessingException {
            UUID userId = UUID.randomUUID();
            UUID createdBy = UUID.randomUUID();
            UserCreatedEvent event = new UserCreatedEvent(UUID.randomUUID(), userId, "jdoe", "jdoe@sirp.local",
                "ENGINEER", createdBy, Instant.now());
            when(objectMapper.writeValueAsString(event)).thenReturn("{}");

            consumer.consumeUserCreated(event);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getAggregateType()).isEqualTo(AggregateType.USER);
            assertThat(captor.getValue().getEventType()).isEqualTo(AuditEventType.USER_CREATED);
            assertThat(captor.getValue().getPerformedBy()).isEqualTo(createdBy);
        }

        @Test
        void consumeUserUpdatedUsesUpdatedBy() throws JsonProcessingException {
            UUID userId = UUID.randomUUID();
            UUID updatedBy = UUID.randomUUID();
            UserUpdatedEvent event = new UserUpdatedEvent(UUID.randomUUID(), userId, "jdoe", "jdoe@sirp.local",
                "ENGINEER", updatedBy, Instant.now());
            when(objectMapper.writeValueAsString(event)).thenReturn("{}");

            consumer.consumeUserUpdated(event);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getEventType()).isEqualTo(AuditEventType.USER_UPDATED);
            assertThat(captor.getValue().getPerformedBy()).isEqualTo(updatedBy);
        }

        @Test
        void consumeUserDeletedUsesDeletedBy() throws JsonProcessingException {
            UUID userId = UUID.randomUUID();
            UUID deletedBy = UUID.randomUUID();
            UserDeletedEvent event = new UserDeletedEvent(UUID.randomUUID(), userId, deletedBy, Instant.now());
            when(objectMapper.writeValueAsString(event)).thenReturn("{}");

            consumer.consumeUserDeleted(event);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getEventType()).isEqualTo(AuditEventType.USER_DELETED);
            assertThat(captor.getValue().getPerformedBy()).isEqualTo(deletedBy);
        }
    }

    @Nested
    class Idempotency {

        @Test
        void skipsSaveWhenEventIdAlreadyPersisted() throws JsonProcessingException {
            UUID eventId = UUID.randomUUID();
            IncidentCreatedEvent event = new IncidentCreatedEvent(eventId, UUID.randomUUID(), "INC-2026-1", "t",
                "d", IncidentPriority.P1, IncidentSeverity.HIGH, UUID.randomUUID(), Instant.now());
            when(repository.existsByEventId(eventId)).thenReturn(true);

            consumer.consumeCreated(event);

            verify(repository, never()).save(any());
        }

        @Test
        void swallowsJsonProcessingExceptionRatherThanPropagating() throws JsonProcessingException {
            IncidentCreatedEvent event = new IncidentCreatedEvent(UUID.randomUUID(), UUID.randomUUID(),
                "INC-2026-1", "t", "d", IncidentPriority.P1, IncidentSeverity.HIGH, UUID.randomUUID(),
                Instant.now());
            when(objectMapper.writeValueAsString(event))
                .thenThrow(new com.fasterxml.jackson.core.JsonGenerationException("boom",
                    (com.fasterxml.jackson.core.JsonGenerator) null));

            org.assertj.core.api.Assertions.assertThatCode(() -> consumer.consumeCreated(event))
                .doesNotThrowAnyException();

            verify(repository, never()).save(any());
        }
    }
}
