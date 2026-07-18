package com.sirp.workflow.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sirp.common.enums.WorkflowStatus;
import com.sirp.common.events.workflow.WorkflowClosedEvent;
import com.sirp.common.events.workflow.WorkflowResolvedEvent;
import com.sirp.workflow.dto.request.CloseWorkflowRequest;
import com.sirp.workflow.dto.request.ResolveWorkflowRequest;
import com.sirp.workflow.dto.response.WorkflowResponse;
import com.sirp.workflow.entity.WorkflowEntity;
import com.sirp.workflow.exception.InvalidWorkflowStateException;
import com.sirp.workflow.exception.WorkflowNotFoundException;
import com.sirp.workflow.feign.ResilientIncidentServiceClient;
import com.sirp.workflow.feign.dto.ResolveIncidentRequest;
import com.sirp.workflow.kafka.producer.WorkflowEventProducer;
import com.sirp.workflow.mapper.WorkflowMapper;
import com.sirp.workflow.repository.WorkflowRepository;
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
class ResolutionServiceImplTest {

    @Mock
    private WorkflowRepository repository;
    @Mock
    private WorkflowMapper mapper;
    @Mock
    private WorkflowEventProducer producer;
    @Mock
    private ResilientIncidentServiceClient incidentServiceClient;

    @InjectMocks
    private ResolutionServiceImpl resolutionService;

    private UUID workflowId;
    private UUID incidentId;
    private UUID actorId;
    private UUID assignee;

    @BeforeEach
    void setUp() {
        workflowId = UUID.randomUUID();
        incidentId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        assignee = UUID.randomUUID();
    }

    @Nested
    class ResolveWorkflow {

        @Test
        void startsThenResolvesUnderlyingIncidentAndPublishesEvent() {
            ResolveWorkflowRequest request = new ResolveWorkflowRequest("Root caused and fixed");
            WorkflowEntity entity = WorkflowEntity.builder().id(workflowId).incidentId(incidentId)
                .assignedTo(assignee).workflowStatus(WorkflowStatus.ESCALATED).build();
            when(repository.findById(workflowId)).thenReturn(Optional.of(entity));
            when(repository.save(entity)).thenReturn(entity);
            when(mapper.toResponse(entity)).thenReturn(
                new WorkflowResponse(workflowId, incidentId, assignee, null, WorkflowStatus.RESOLVED, null, 1,
                    null, null, entity.getResolvedAt(), null, "Root caused and fixed", null, null));

            resolutionService.resolveWorkflow(workflowId, request, actorId);

            assertThat(entity.getWorkflowStatus()).isEqualTo(WorkflowStatus.RESOLVED);
            assertThat(entity.getResolvedAt()).isNotNull();
            assertThat(entity.getRemarks()).isEqualTo("Root caused and fixed");

            verify(incidentServiceClient).startIncident(incidentId);
            verify(incidentServiceClient).resolveIncident(incidentId,
                new ResolveIncidentRequest("Root caused and fixed"));

            ArgumentCaptor<WorkflowResolvedEvent> eventCaptor = ArgumentCaptor.forClass(WorkflowResolvedEvent.class);
            verify(producer).publishWorkflowResolved(eventCaptor.capture());
            assertThat(eventCaptor.getValue().resolvedBy()).isEqualTo(actorId);
        }

        @Test
        void fallsBackToAssignedToWhenActorIdNull() {
            ResolveWorkflowRequest request = new ResolveWorkflowRequest("Root caused and fixed");
            WorkflowEntity entity = WorkflowEntity.builder().id(workflowId).incidentId(incidentId)
                .assignedTo(assignee).workflowStatus(WorkflowStatus.ASSIGNED).build();
            when(repository.findById(workflowId)).thenReturn(Optional.of(entity));
            when(repository.save(entity)).thenReturn(entity);
            when(mapper.toResponse(entity)).thenReturn(
                new WorkflowResponse(workflowId, incidentId, assignee, null, WorkflowStatus.RESOLVED, null, 0,
                    null, null, entity.getResolvedAt(), null, "Root caused and fixed", null, null));

            resolutionService.resolveWorkflow(workflowId, request, null);

            ArgumentCaptor<WorkflowResolvedEvent> eventCaptor = ArgumentCaptor.forClass(WorkflowResolvedEvent.class);
            verify(producer).publishWorkflowResolved(eventCaptor.capture());
            assertThat(eventCaptor.getValue().resolvedBy()).isEqualTo(assignee);
        }

        @ParameterizedTest
        @EnumSource(value = WorkflowStatus.class, names = {"RESOLVED", "CLOSED"})
        void throwsWhenAlreadyTerminal(WorkflowStatus terminalStatus) {
            ResolveWorkflowRequest request = new ResolveWorkflowRequest("Root caused and fixed");
            WorkflowEntity entity = WorkflowEntity.builder().id(workflowId).incidentId(incidentId)
                .workflowStatus(terminalStatus).build();
            when(repository.findById(workflowId)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> resolutionService.resolveWorkflow(workflowId, request, actorId))
                .isInstanceOf(InvalidWorkflowStateException.class);

            verify(incidentServiceClient, never()).startIncident(any());
            verify(incidentServiceClient, never()).resolveIncident(any(), any());
        }

        @Test
        void throwsWhenWorkflowMissing() {
            ResolveWorkflowRequest request = new ResolveWorkflowRequest("Root caused and fixed");
            when(repository.findById(workflowId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> resolutionService.resolveWorkflow(workflowId, request, actorId))
                .isInstanceOf(WorkflowNotFoundException.class);
        }
    }

    @Nested
    class CloseWorkflow {

        @Test
        void closesUnderlyingIncidentAndPublishesEventWhenAlreadyResolved() {
            CloseWorkflowRequest request = new CloseWorkflowRequest("All good now");
            WorkflowEntity entity = WorkflowEntity.builder().id(workflowId).incidentId(incidentId)
                .assignedTo(assignee).workflowStatus(WorkflowStatus.RESOLVED).build();
            when(repository.findById(workflowId)).thenReturn(Optional.of(entity));
            when(repository.save(entity)).thenReturn(entity);
            when(mapper.toResponse(entity)).thenReturn(
                new WorkflowResponse(workflowId, incidentId, assignee, null, WorkflowStatus.CLOSED, null, 0, null,
                    null, null, entity.getClosedAt(), "All good now", null, null));

            resolutionService.closeWorkflow(workflowId, request, actorId);

            assertThat(entity.getWorkflowStatus()).isEqualTo(WorkflowStatus.CLOSED);
            assertThat(entity.getClosedAt()).isNotNull();
            verify(incidentServiceClient).closeIncident(incidentId);

            ArgumentCaptor<WorkflowClosedEvent> eventCaptor = ArgumentCaptor.forClass(WorkflowClosedEvent.class);
            verify(producer).publishWorkflowClosed(eventCaptor.capture());
            assertThat(eventCaptor.getValue().closedBy()).isEqualTo(actorId);
        }

        @ParameterizedTest
        @EnumSource(value = WorkflowStatus.class, names = "RESOLVED", mode = EnumSource.Mode.EXCLUDE)
        void throwsUnlessCurrentlyResolved(WorkflowStatus nonResolvedStatus) {
            CloseWorkflowRequest request = new CloseWorkflowRequest("All good now");
            WorkflowEntity entity = WorkflowEntity.builder().id(workflowId).incidentId(incidentId)
                .workflowStatus(nonResolvedStatus).build();
            when(repository.findById(workflowId)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> resolutionService.closeWorkflow(workflowId, request, actorId))
                .isInstanceOf(InvalidWorkflowStateException.class);

            verify(incidentServiceClient, never()).closeIncident(any());
        }

        @Test
        void throwsWhenWorkflowMissing() {
            CloseWorkflowRequest request = new CloseWorkflowRequest("All good now");
            when(repository.findById(workflowId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> resolutionService.closeWorkflow(workflowId, request, actorId))
                .isInstanceOf(WorkflowNotFoundException.class);
        }
    }
}
