package com.sirp.workflow.service.impl;

import com.sirp.common.enums.WorkflowStatus;
import com.sirp.common.events.workflow.WorkflowAssignedEvent;
import com.sirp.workflow.dto.request.AssignWorkflowRequest;
import com.sirp.workflow.dto.response.WorkflowResponse;
import com.sirp.workflow.entity.WorkflowEntity;
import com.sirp.workflow.exception.InvalidWorkflowStateException;
import com.sirp.workflow.exception.WorkflowNotFoundException;
import com.sirp.workflow.feign.IncidentServiceClient;
import com.sirp.workflow.feign.dto.AssignIncidentRequest;
import com.sirp.workflow.kafka.producer.WorkflowEventProducer;
import com.sirp.workflow.mapper.WorkflowMapper;
import com.sirp.workflow.repository.WorkflowRepository;
import com.sirp.workflow.service.AssignmentService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentServiceImpl implements AssignmentService {

    private static final Set<WorkflowStatus> TERMINAL_STATUSES =
        Set.of(WorkflowStatus.RESOLVED, WorkflowStatus.CLOSED);

    private final WorkflowRepository repository;
    private final WorkflowMapper mapper;
    private final WorkflowEventProducer producer;
    private final IncidentServiceClient incidentServiceClient;

    @Override
    public WorkflowResponse assignWorkflow(UUID workflowId, AssignWorkflowRequest request) {
        WorkflowEntity entity = findOrThrow(workflowId);
        assertNotTerminal(entity);
        return doAssign(entity, request);
    }

    @Override
    public WorkflowResponse reassignWorkflow(UUID workflowId, AssignWorkflowRequest request) {
        WorkflowEntity entity = findOrThrow(workflowId);
        assertNotTerminal(entity);
        if (entity.getAssignedTo() == null) {
            throw new InvalidWorkflowStateException(
                "Workflow " + workflowId + " has not been assigned yet; use assign instead.");
        }
        return doAssign(entity, request);
    }

    private WorkflowResponse doAssign(WorkflowEntity entity, AssignWorkflowRequest request) {
        entity.setAssignedTo(request.assignedTo());
        entity.setAssignedTeam(request.assignedTeam());
        entity.setWorkflowStatus(WorkflowStatus.ASSIGNED);
        WorkflowEntity saved = repository.save(entity);

        incidentServiceClient.assignIncident(saved.getIncidentId(),
            new AssignIncidentRequest(saved.getAssignedTo()));

        producer.publishWorkflowAssigned(new WorkflowAssignedEvent(UUID.randomUUID(), saved.getId(),
                                                                    saved.getIncidentId(), saved.getAssignedTo(),
                                                                    saved.getAssignedTeam(),
                                                                    toLocalDateTime(Instant.now())));

        return mapper.toResponse(saved);
    }

    private void assertNotTerminal(WorkflowEntity entity) {
        if (TERMINAL_STATUSES.contains(entity.getWorkflowStatus())) {
            throw new InvalidWorkflowStateException(
                "Cannot assign workflow " + entity.getId() + " in terminal state " + entity.getWorkflowStatus());
        }
    }

    private WorkflowEntity findOrThrow(UUID workflowId) {
        return repository.findById(workflowId)
                         .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found: " + workflowId));
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
