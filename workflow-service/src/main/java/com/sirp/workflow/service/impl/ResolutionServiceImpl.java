package com.sirp.workflow.service.impl;

import com.sirp.common.enums.WorkflowStatus;
import com.sirp.common.events.workflow.WorkflowClosedEvent;
import com.sirp.common.events.workflow.WorkflowResolvedEvent;
import com.sirp.workflow.dto.request.CloseWorkflowRequest;
import com.sirp.workflow.dto.request.ResolveWorkflowRequest;
import com.sirp.workflow.dto.response.WorkflowResponse;
import com.sirp.workflow.entity.WorkflowEntity;
import com.sirp.workflow.exception.InvalidWorkflowStateException;
import com.sirp.workflow.exception.WorkflowNotFoundException;
import com.sirp.workflow.feign.IncidentServiceClient;
import com.sirp.workflow.feign.dto.ResolveIncidentRequest;
import com.sirp.workflow.kafka.producer.WorkflowEventProducer;
import com.sirp.workflow.mapper.WorkflowMapper;
import com.sirp.workflow.repository.WorkflowRepository;
import com.sirp.workflow.service.ResolutionService;
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
public class ResolutionServiceImpl implements ResolutionService {

    private static final Set<WorkflowStatus> TERMINAL_STATUSES =
        Set.of(WorkflowStatus.RESOLVED, WorkflowStatus.CLOSED);

    private final WorkflowRepository repository;
    private final WorkflowMapper mapper;
    private final WorkflowEventProducer producer;
    private final IncidentServiceClient incidentServiceClient;

    @Override
    public WorkflowResponse resolveWorkflow(UUID workflowId, ResolveWorkflowRequest request) {
        WorkflowEntity entity = findOrThrow(workflowId);
        if (TERMINAL_STATUSES.contains(entity.getWorkflowStatus())) {
            throw new InvalidWorkflowStateException(
                "Cannot resolve workflow " + workflowId + " in terminal state " + entity.getWorkflowStatus());
        }

        Instant now = Instant.now();
        entity.setResolvedAt(now);
        entity.setRemarks(request.remarks());
        entity.setWorkflowStatus(WorkflowStatus.RESOLVED);
        WorkflowEntity saved = repository.save(entity);

        incidentServiceClient.resolveIncident(saved.getIncidentId(), new ResolveIncidentRequest(request.remarks()));

        // The assignee is the closest thing to an "acting user" available here -
        // workflow-service has no authenticated principal of its own yet.
        producer.publishWorkflowResolved(new WorkflowResolvedEvent(UUID.randomUUID(), saved.getId(),
                                                                    saved.getIncidentId(), saved.getAssignedTo(),
                                                                    toLocalDateTime(now), request.remarks()));

        return mapper.toResponse(saved);
    }

    @Override
    public WorkflowResponse closeWorkflow(UUID workflowId, CloseWorkflowRequest request) {
        WorkflowEntity entity = findOrThrow(workflowId);
        if (entity.getWorkflowStatus() != WorkflowStatus.RESOLVED) {
            throw new InvalidWorkflowStateException(
                "Workflow " + workflowId + " must be RESOLVED before it can be closed (current state: "
                    + entity.getWorkflowStatus() + ")");
        }

        Instant now = Instant.now();
        entity.setClosedAt(now);
        entity.setRemarks(request.remarks());
        entity.setWorkflowStatus(WorkflowStatus.CLOSED);
        WorkflowEntity saved = repository.save(entity);

        incidentServiceClient.closeIncident(saved.getIncidentId());

        producer.publishWorkflowClosed(
            new WorkflowClosedEvent(UUID.randomUUID(), saved.getId(), saved.getIncidentId(), toLocalDateTime(now)));

        return mapper.toResponse(saved);
    }

    private WorkflowEntity findOrThrow(UUID workflowId) {
        return repository.findById(workflowId)
                         .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found: " + workflowId));
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
