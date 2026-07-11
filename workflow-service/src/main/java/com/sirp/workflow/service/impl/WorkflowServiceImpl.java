package com.sirp.workflow.service.impl;

import com.sirp.workflow.dto.request.CreateWorkflowRequest;
import com.sirp.workflow.dto.response.WorkflowResponse;
import com.sirp.workflow.entity.WorkflowEntity;
import com.sirp.workflow.exception.WorkflowAlreadyExistsException;
import com.sirp.workflow.exception.WorkflowNotFoundException;
import com.sirp.workflow.kafka.producer.WorkflowEventProducer;
import com.sirp.workflow.mapper.WorkflowMapper;
import com.sirp.workflow.repository.WorkflowRepository;
import com.sirp.workflow.service.WorkflowService;
import com.sirp.common.enums.WorkflowStatus;
import com.sirp.common.events.workflow.WorkflowCreatedEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowRepository repository;
    private final WorkflowMapper mapper;
    private final WorkflowEventProducer producer;

    @Override
    public WorkflowResponse createWorkflow(CreateWorkflowRequest request) {
        if (repository.existsByIncidentId(request.incidentId())) {
            throw new WorkflowAlreadyExistsException(
                "A workflow already exists for incident " + request.incidentId());
        }

        WorkflowEntity entity = mapper.toEntity(request);
        if (entity.getAssignedTo() != null) {
            entity.setWorkflowStatus(WorkflowStatus.ASSIGNED);
        }

        WorkflowEntity saved = repository.save(entity);

        producer.publishWorkflowCreated(new WorkflowCreatedEvent(UUID.randomUUID(), saved.getId(),
                                                                 saved.getIncidentId(), saved.getAssignedTo(),
                                                                 saved.getAssignedTeam(), saved.getSeverity(),
                                                                 saved.getEscalationLevel(),
                                                                 toLocalDateTime(saved.getSlaDeadline()),
                                                                 toLocalDateTime(saved.getNextEscalationTime()),
                                                                 toLocalDateTime(saved.getCreatedAt())));

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflowById(UUID workflowId) {
        return mapper.toResponse(findOrThrow(workflowId));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflowByIncidentId(UUID incidentId) {
        WorkflowEntity entity = repository.findByIncidentId(incidentId)
                                          .orElseThrow(() -> new WorkflowNotFoundException(
                                              "No workflow found for incident " + incidentId));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkflowResponse> getAllWorkflows(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowResponse> getWorkflowsByAssignedUser(UUID userId) {
        return repository.findByAssignedTo(userId).stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowResponse> getWorkflowsByAssignedTeam(UUID teamId) {
        return repository.findByAssignedTeam(teamId).stream().map(mapper::toResponse).toList();
    }

    @Override
    public void deleteWorkflow(UUID workflowId) {
        WorkflowEntity entity = findOrThrow(workflowId);
        repository.delete(entity);
    }

    private WorkflowEntity findOrThrow(UUID workflowId) {
        return repository.findById(workflowId)
                         .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found: " + workflowId));
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
