package com.sirp.workflow.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sirp.common.enums.IncidentSeverity;
import com.sirp.common.enums.WorkflowStatus;
import com.sirp.common.events.workflow.WorkflowCreatedEvent;
import com.sirp.workflow.dto.request.CreateWorkflowRequest;
import com.sirp.workflow.dto.response.WorkflowResponse;
import com.sirp.workflow.entity.WorkflowEntity;
import com.sirp.workflow.exception.WorkflowAlreadyExistsException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceImplTest {

    @Mock
    private WorkflowRepository repository;
    @Mock
    private WorkflowMapper mapper;
    @Mock
    private WorkflowEventProducer producer;

    @InjectMocks
    private WorkflowServiceImpl workflowService;

    private UUID incidentId;
    private UUID actorId;
    private UUID workflowId;

    @BeforeEach
    void setUp() {
        incidentId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        workflowId = UUID.randomUUID();
    }

    @Nested
    class CreateWorkflow {

        @Test
        void savesAsCreatedAndPublishesEventWhenNoAssignee() {
            CreateWorkflowRequest request = new CreateWorkflowRequest(incidentId, null, null,
                IncidentSeverity.HIGH, Instant.now().plusSeconds(3600), null, null);
            WorkflowEntity mappedEntity = WorkflowEntity.builder().incidentId(incidentId)
                .workflowStatus(WorkflowStatus.CREATED).severity(IncidentSeverity.HIGH).escalationLevel(0).build();
            WorkflowEntity saved = WorkflowEntity.builder().id(workflowId).incidentId(incidentId)
                .workflowStatus(WorkflowStatus.CREATED).severity(IncidentSeverity.HIGH).escalationLevel(0)
                .createdAt(Instant.now()).build();
            WorkflowResponse response = new WorkflowResponse(workflowId, incidentId, null, null,
                WorkflowStatus.CREATED, IncidentSeverity.HIGH, 0, request.slaDeadline(), null, null, null, null,
                Instant.now(), Instant.now());

            when(repository.existsByIncidentId(incidentId)).thenReturn(false);
            when(mapper.toEntity(request)).thenReturn(mappedEntity);
            when(repository.save(mappedEntity)).thenReturn(saved);
            when(mapper.toResponse(saved)).thenReturn(response);

            WorkflowResponse result = workflowService.createWorkflow(request, actorId);

            assertThat(result).isEqualTo(response);
            assertThat(mappedEntity.getWorkflowStatus()).isEqualTo(WorkflowStatus.CREATED);
            ArgumentCaptor<WorkflowCreatedEvent> eventCaptor = ArgumentCaptor.forClass(WorkflowCreatedEvent.class);
            verify(producer).publishWorkflowCreated(eventCaptor.capture());
            assertThat(eventCaptor.getValue().workflowId()).isEqualTo(workflowId);
            assertThat(eventCaptor.getValue().createdBy()).isEqualTo(actorId);
        }

        @Test
        void movesToAssignedWhenAssigneeProvidedAtCreation() {
            UUID assignee = UUID.randomUUID();
            CreateWorkflowRequest request = new CreateWorkflowRequest(incidentId, assignee, null,
                IncidentSeverity.HIGH, Instant.now().plusSeconds(3600), null, null);
            WorkflowEntity mappedEntity = WorkflowEntity.builder().incidentId(incidentId).assignedTo(assignee)
                .workflowStatus(WorkflowStatus.CREATED).severity(IncidentSeverity.HIGH).escalationLevel(0).build();
            when(repository.existsByIncidentId(incidentId)).thenReturn(false);
            when(mapper.toEntity(request)).thenReturn(mappedEntity);
            when(repository.save(mappedEntity)).thenReturn(mappedEntity);
            when(mapper.toResponse(mappedEntity)).thenReturn(
                new WorkflowResponse(workflowId, incidentId, assignee, null, WorkflowStatus.ASSIGNED,
                    IncidentSeverity.HIGH, 0, request.slaDeadline(), null, null, null, null, Instant.now(),
                    Instant.now()));

            workflowService.createWorkflow(request, actorId);

            assertThat(mappedEntity.getWorkflowStatus()).isEqualTo(WorkflowStatus.ASSIGNED);
        }

        @Test
        void throwsWhenWorkflowAlreadyExistsForIncident() {
            CreateWorkflowRequest request = new CreateWorkflowRequest(incidentId, null, null,
                IncidentSeverity.HIGH, Instant.now().plusSeconds(3600), null, null);
            when(repository.existsByIncidentId(incidentId)).thenReturn(true);

            assertThatThrownBy(() -> workflowService.createWorkflow(request, actorId))
                .isInstanceOf(WorkflowAlreadyExistsException.class);

            verify(repository, never()).save(any());
            verify(producer, never()).publishWorkflowCreated(any());
        }
    }

    @Nested
    class GetWorkflowById {

        @Test
        void returnsMappedResponseWhenFound() {
            WorkflowEntity entity = WorkflowEntity.builder().id(workflowId).incidentId(incidentId).build();
            WorkflowResponse response = new WorkflowResponse(workflowId, incidentId, null, null,
                WorkflowStatus.CREATED, IncidentSeverity.HIGH, 0, null, null, null, null, null, null, null);
            when(repository.findById(workflowId)).thenReturn(Optional.of(entity));
            when(mapper.toResponse(entity)).thenReturn(response);

            assertThat(workflowService.getWorkflowById(workflowId)).isEqualTo(response);
        }

        @Test
        void throwsWhenMissing() {
            when(repository.findById(workflowId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> workflowService.getWorkflowById(workflowId))
                .isInstanceOf(WorkflowNotFoundException.class);
        }
    }

    @Nested
    class GetWorkflowByIncidentId {

        @Test
        void returnsMappedResponseWhenFound() {
            WorkflowEntity entity = WorkflowEntity.builder().id(workflowId).incidentId(incidentId).build();
            WorkflowResponse response = new WorkflowResponse(workflowId, incidentId, null, null,
                WorkflowStatus.CREATED, IncidentSeverity.HIGH, 0, null, null, null, null, null, null, null);
            when(repository.findByIncidentId(incidentId)).thenReturn(Optional.of(entity));
            when(mapper.toResponse(entity)).thenReturn(response);

            assertThat(workflowService.getWorkflowByIncidentId(incidentId)).isEqualTo(response);
        }

        @Test
        void throwsWhenNoWorkflowForIncident() {
            when(repository.findByIncidentId(incidentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> workflowService.getWorkflowByIncidentId(incidentId))
                .isInstanceOf(WorkflowNotFoundException.class);
        }
    }

    @Test
    void getAllWorkflowsMapsEveryPageEntry() {
        WorkflowEntity entity = WorkflowEntity.builder().id(workflowId).incidentId(incidentId).build();
        WorkflowResponse response = new WorkflowResponse(workflowId, incidentId, null, null,
            WorkflowStatus.CREATED, IncidentSeverity.HIGH, 0, null, null, null, null, null, null, null);
        Page<WorkflowEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1);
        when(repository.findAll(PageRequest.of(0, 20))).thenReturn(page);
        when(mapper.toResponse(entity)).thenReturn(response);

        Page<WorkflowResponse> result = workflowService.getAllWorkflows(PageRequest.of(0, 20));

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void getWorkflowsByAssignedUserReturnsPlainList() {
        UUID userId = UUID.randomUUID();
        WorkflowEntity entity = WorkflowEntity.builder().id(workflowId).incidentId(incidentId)
            .assignedTo(userId).build();
        WorkflowResponse response = new WorkflowResponse(workflowId, incidentId, userId, null,
            WorkflowStatus.ASSIGNED, IncidentSeverity.HIGH, 0, null, null, null, null, null, null, null);
        when(repository.findByAssignedTo(userId)).thenReturn(List.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        assertThat(workflowService.getWorkflowsByAssignedUser(userId)).containsExactly(response);
    }

    @Test
    void getWorkflowsByAssignedTeamReturnsPlainList() {
        UUID teamId = UUID.randomUUID();
        WorkflowEntity entity = WorkflowEntity.builder().id(workflowId).incidentId(incidentId)
            .assignedTeam(teamId).build();
        WorkflowResponse response = new WorkflowResponse(workflowId, incidentId, null, teamId,
            WorkflowStatus.CREATED, IncidentSeverity.HIGH, 0, null, null, null, null, null, null, null);
        when(repository.findByAssignedTeam(teamId)).thenReturn(List.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        assertThat(workflowService.getWorkflowsByAssignedTeam(teamId)).containsExactly(response);
    }

    @Nested
    class DeleteWorkflow {

        @Test
        void deletesWhenFound() {
            WorkflowEntity entity = WorkflowEntity.builder().id(workflowId).incidentId(incidentId).build();
            when(repository.findById(workflowId)).thenReturn(Optional.of(entity));

            workflowService.deleteWorkflow(workflowId);

            verify(repository, times(1)).delete(entity);
        }

        @Test
        void throwsWhenMissing() {
            when(repository.findById(workflowId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> workflowService.deleteWorkflow(workflowId))
                .isInstanceOf(WorkflowNotFoundException.class);

            verify(repository, never()).delete(any());
        }
    }
}
