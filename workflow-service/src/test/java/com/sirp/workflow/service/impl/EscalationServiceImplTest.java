package com.sirp.workflow.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sirp.common.enums.WorkflowStatus;
import com.sirp.common.events.workflow.WorkflowEscalatedEvent;
import com.sirp.workflow.dto.request.EscalateWorkflowRequest;
import com.sirp.workflow.dto.response.WorkflowResponse;
import com.sirp.workflow.entity.WorkflowEntity;
import com.sirp.workflow.exception.InvalidWorkflowStateException;
import com.sirp.workflow.exception.WorkflowNotFoundException;
import com.sirp.workflow.kafka.producer.WorkflowEventProducer;
import com.sirp.workflow.mapper.WorkflowMapper;
import com.sirp.workflow.repository.WorkflowRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EscalationServiceImplTest {

    @Mock
    private WorkflowRepository repository;
    @Mock
    private WorkflowMapper mapper;
    @Mock
    private WorkflowEventProducer producer;

    @InjectMocks
    private EscalationServiceImpl escalationService;

    private UUID workflowId;
    private UUID incidentId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        workflowId = UUID.randomUUID();
        incidentId = UUID.randomUUID();
        actorId = UUID.randomUUID();
    }

    @Nested
    class EscalateWorkflow {

        @Test
        void incrementsLevelSetsEscalatedAndPublishesEvent() {
            EscalateWorkflowRequest request = new EscalateWorkflowRequest(Instant.now().plusSeconds(1800),
                "Still not fixed");
            WorkflowEntity entity = WorkflowEntity.builder().id(workflowId).incidentId(incidentId)
                .workflowStatus(WorkflowStatus.ASSIGNED).escalationLevel(1).build();
            when(repository.findById(workflowId)).thenReturn(Optional.of(entity));
            when(repository.save(entity)).thenReturn(entity);
            when(mapper.toResponse(entity)).thenReturn(
                new WorkflowResponse(workflowId, incidentId, null, null, WorkflowStatus.ESCALATED, null, 2, null,
                    request.nextEscalationTime(), null, null, "Still not fixed", null, null));

            WorkflowResponse result = escalationService.escalateWorkflow(workflowId, request, actorId);

            assertThat(result.escalationLevel()).isEqualTo(2);
            assertThat(entity.getEscalationLevel()).isEqualTo(2);
            assertThat(entity.getWorkflowStatus()).isEqualTo(WorkflowStatus.ESCALATED);
            assertThat(entity.getRemarks()).isEqualTo("Still not fixed");

            ArgumentCaptor<WorkflowEscalatedEvent> eventCaptor =
                ArgumentCaptor.forClass(WorkflowEscalatedEvent.class);
            verify(producer).publishWorkflowEscalated(eventCaptor.capture());
            assertThat(eventCaptor.getValue().escalatedBy()).isEqualTo(actorId);
            assertThat(eventCaptor.getValue().escalationLevel()).isEqualTo(2);
        }

        @ParameterizedTest
        @EnumSource(value = WorkflowStatus.class, names = {"RESOLVED", "CLOSED"})
        void throwsWhenWorkflowAlreadyTerminal(WorkflowStatus terminalStatus) {
            EscalateWorkflowRequest request = new EscalateWorkflowRequest(Instant.now().plusSeconds(1800), null);
            WorkflowEntity entity = WorkflowEntity.builder().id(workflowId).incidentId(incidentId)
                .workflowStatus(terminalStatus).escalationLevel(1).build();
            when(repository.findById(workflowId)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> escalationService.escalateWorkflow(workflowId, request, actorId))
                .isInstanceOf(InvalidWorkflowStateException.class);

            verify(repository, never()).save(any());
        }

        @Test
        void throwsWhenWorkflowMissing() {
            EscalateWorkflowRequest request = new EscalateWorkflowRequest(Instant.now().plusSeconds(1800), null);
            when(repository.findById(workflowId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> escalationService.escalateWorkflow(workflowId, request, actorId))
                .isInstanceOf(WorkflowNotFoundException.class);
        }
    }

    @Nested
    class ProcessScheduledEscalations {

        @Test
        void autoEscalatesOverdueWorkflowsAcrossAllThreeEscalatableStatuses() {
            WorkflowEntity assignedOverdue = WorkflowEntity.builder().id(UUID.randomUUID())
                .incidentId(UUID.randomUUID()).workflowStatus(WorkflowStatus.ASSIGNED).escalationLevel(0).build();
            WorkflowEntity escalatedOverdue = WorkflowEntity.builder().id(UUID.randomUUID())
                .incidentId(UUID.randomUUID()).workflowStatus(WorkflowStatus.ESCALATED).escalationLevel(2).build();
            when(repository.findByNextEscalationTimeBeforeAndWorkflowStatus(any(Instant.class),
                eq(WorkflowStatus.ASSIGNED))).thenReturn(List.of(assignedOverdue));
            when(repository.findByNextEscalationTimeBeforeAndWorkflowStatus(any(Instant.class),
                eq(WorkflowStatus.IN_PROGRESS))).thenReturn(List.of());
            when(repository.findByNextEscalationTimeBeforeAndWorkflowStatus(any(Instant.class),
                eq(WorkflowStatus.ESCALATED))).thenReturn(List.of(escalatedOverdue));
            when(repository.save(any(WorkflowEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            escalationService.processScheduledEscalations();

            assertThat(assignedOverdue.getEscalationLevel()).isEqualTo(1);
            assertThat(assignedOverdue.getWorkflowStatus()).isEqualTo(WorkflowStatus.ESCALATED);
            assertThat(assignedOverdue.getNextEscalationTime()).isAfter(Instant.now().plusSeconds(1700));
            assertThat(assignedOverdue.getRemarks()).contains("Auto-escalated");

            assertThat(escalatedOverdue.getEscalationLevel()).isEqualTo(3);

            ArgumentCaptor<WorkflowEscalatedEvent> eventCaptor =
                ArgumentCaptor.forClass(WorkflowEscalatedEvent.class);
            verify(producer, times(2)).publishWorkflowEscalated(eventCaptor.capture());
            assertThat(eventCaptor.getAllValues()).allSatisfy(event -> assertThat(event.escalatedBy()).isNull());
        }

        @Test
        void doesNothingWhenNoWorkflowsAreOverdue() {
            when(repository.findByNextEscalationTimeBeforeAndWorkflowStatus(any(Instant.class), any()))
                .thenReturn(List.of());

            escalationService.processScheduledEscalations();

            verify(repository, never()).save(any());
            verify(producer, never()).publishWorkflowEscalated(any());
        }
    }
}
