package com.sirp.workflow.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sirp.common.enums.IncidentSeverity;
import com.sirp.common.enums.WorkflowStatus;
import com.sirp.common.events.workflow.WorkflowAssignedEvent;
import com.sirp.workflow.dto.request.AssignWorkflowRequest;
import com.sirp.workflow.dto.response.WorkflowResponse;
import com.sirp.workflow.entity.WorkflowEntity;
import com.sirp.workflow.exception.InvalidWorkflowStateException;
import com.sirp.workflow.exception.WorkflowNotFoundException;
import com.sirp.workflow.feign.ResilientIncidentServiceClient;
import com.sirp.workflow.feign.dto.AssignIncidentRequest;
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
class AssignmentServiceImplTest {

    @Mock
    private WorkflowRepository repository;
    @Mock
    private WorkflowMapper mapper;
    @Mock
    private WorkflowEventProducer producer;
    @Mock
    private ResilientIncidentServiceClient incidentServiceClient;

    @InjectMocks
    private AssignmentServiceImpl assignmentService;

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
    class AssignWorkflow {

        @Test
        void assignsNotifiesIncidentServiceAndPublishesEvent() {
            AssignWorkflowRequest request = new AssignWorkflowRequest(assignee, null);
            WorkflowEntity entity = WorkflowEntity.builder().id(workflowId).incidentId(incidentId)
                .workflowStatus(WorkflowStatus.CREATED).severity(IncidentSeverity.HIGH).build();
            when(repository.findById(workflowId)).thenReturn(Optional.of(entity));
            when(repository.save(entity)).thenReturn(entity);
            when(mapper.toResponse(entity)).thenReturn(
                new WorkflowResponse(workflowId, incidentId, assignee, null, WorkflowStatus.ASSIGNED,
                    IncidentSeverity.HIGH, 0, null, null, null, null, null, null, null));

            WorkflowResponse result = assignmentService.assignWorkflow(workflowId, request, actorId);

            assertThat(result.workflowStatus()).isEqualTo(WorkflowStatus.ASSIGNED);
            assertThat(entity.getAssignedTo()).isEqualTo(assignee);
            assertThat(entity.getWorkflowStatus()).isEqualTo(WorkflowStatus.ASSIGNED);
            verify(incidentServiceClient).assignIncident(incidentId, new AssignIncidentRequest(assignee));

            ArgumentCaptor<WorkflowAssignedEvent> eventCaptor = ArgumentCaptor.forClass(WorkflowAssignedEvent.class);
            verify(producer).publishWorkflowAssigned(eventCaptor.capture());
            assertThat(eventCaptor.getValue().assignedBy()).isEqualTo(actorId);
            assertThat(eventCaptor.getValue().assignedTo()).isEqualTo(assignee);
        }

        @ParameterizedTest
        @EnumSource(value = WorkflowStatus.class, names = {"RESOLVED", "CLOSED"})
        void throwsWhenWorkflowAlreadyTerminal(WorkflowStatus terminalStatus) {
            AssignWorkflowRequest request = new AssignWorkflowRequest(assignee, null);
            WorkflowEntity entity = WorkflowEntity.builder().id(workflowId).incidentId(incidentId)
                .workflowStatus(terminalStatus).build();
            when(repository.findById(workflowId)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> assignmentService.assignWorkflow(workflowId, request, actorId))
                .isInstanceOf(InvalidWorkflowStateException.class);

            verify(repository, never()).save(any());
            verify(incidentServiceClient, never()).assignIncident(any(), any());
        }

        @Test
        void throwsWhenWorkflowMissing() {
            AssignWorkflowRequest request = new AssignWorkflowRequest(assignee, null);
            when(repository.findById(workflowId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> assignmentService.assignWorkflow(workflowId, request, actorId))
                .isInstanceOf(WorkflowNotFoundException.class);
        }
    }

    @Nested
    class ReassignWorkflow {

        @Test
        void reassignsWhenPreviouslyAssigned() {
            UUID newAssignee = UUID.randomUUID();
            AssignWorkflowRequest request = new AssignWorkflowRequest(newAssignee, null);
            WorkflowEntity entity = WorkflowEntity.builder().id(workflowId).incidentId(incidentId)
                .assignedTo(assignee).workflowStatus(WorkflowStatus.ASSIGNED).build();
            when(repository.findById(workflowId)).thenReturn(Optional.of(entity));
            when(repository.save(entity)).thenReturn(entity);
            when(mapper.toResponse(entity)).thenReturn(
                new WorkflowResponse(workflowId, incidentId, newAssignee, null, WorkflowStatus.ASSIGNED,
                    IncidentSeverity.HIGH, 0, null, null, null, null, null, null, null));

            assignmentService.reassignWorkflow(workflowId, request, actorId);

            assertThat(entity.getAssignedTo()).isEqualTo(newAssignee);
            verify(incidentServiceClient).assignIncident(incidentId, new AssignIncidentRequest(newAssignee));
        }

        @Test
        void throwsWhenNeverAssignedBefore() {
            AssignWorkflowRequest request = new AssignWorkflowRequest(assignee, null);
            WorkflowEntity entity = WorkflowEntity.builder().id(workflowId).incidentId(incidentId)
                .assignedTo(null).workflowStatus(WorkflowStatus.CREATED).build();
            when(repository.findById(workflowId)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> assignmentService.reassignWorkflow(workflowId, request, actorId))
                .isInstanceOf(InvalidWorkflowStateException.class)
                .hasMessageContaining("use assign instead");

            verify(repository, never()).save(any());
        }

        @ParameterizedTest
        @EnumSource(value = WorkflowStatus.class, names = {"RESOLVED", "CLOSED"})
        void throwsWhenWorkflowAlreadyTerminal(WorkflowStatus terminalStatus) {
            AssignWorkflowRequest request = new AssignWorkflowRequest(assignee, null);
            WorkflowEntity entity = WorkflowEntity.builder().id(workflowId).incidentId(incidentId)
                .assignedTo(assignee).workflowStatus(terminalStatus).build();
            when(repository.findById(workflowId)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> assignmentService.reassignWorkflow(workflowId, request, actorId))
                .isInstanceOf(InvalidWorkflowStateException.class);
        }
    }
}
