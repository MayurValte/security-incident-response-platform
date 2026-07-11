package com.sirp.workflow.service.impl;

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
import com.sirp.workflow.service.EscalationService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EscalationServiceImpl implements EscalationService {

    private static final Set<WorkflowStatus> TERMINAL_STATUSES =
        Set.of(WorkflowStatus.RESOLVED, WorkflowStatus.CLOSED);

    private static final List<WorkflowStatus> ESCALATABLE_STATUSES =
        List.of(WorkflowStatus.ASSIGNED, WorkflowStatus.IN_PROGRESS, WorkflowStatus.ESCALATED);

    private static final long AUTO_ESCALATION_BACKOFF_MINUTES = 30;

    private final WorkflowRepository repository;
    private final WorkflowMapper mapper;
    private final WorkflowEventProducer producer;

    @Override
    public WorkflowResponse escalateWorkflow(UUID workflowId, EscalateWorkflowRequest request) {
        WorkflowEntity entity = repository.findById(workflowId)
                                          .orElseThrow(() -> new WorkflowNotFoundException(
                                              "Workflow not found: " + workflowId));

        if (TERMINAL_STATUSES.contains(entity.getWorkflowStatus())) {
            throw new InvalidWorkflowStateException(
                "Cannot escalate workflow " + workflowId + " in terminal state " + entity.getWorkflowStatus());
        }

        entity.setEscalationLevel(entity.getEscalationLevel() + 1);
        entity.setNextEscalationTime(request.nextEscalationTime());
        entity.setRemarks(request.remarks());
        entity.setWorkflowStatus(WorkflowStatus.ESCALATED);
        WorkflowEntity saved = repository.save(entity);

        publishEscalated(saved);

        return mapper.toResponse(saved);
    }

    @Override
    @Scheduled(fixedRateString = "${workflow.escalation.check-interval-ms:300000}")
    public void processScheduledEscalations() {
        Instant now = Instant.now();
        for (WorkflowStatus status : ESCALATABLE_STATUSES) {
            List<WorkflowEntity> overdue =
                repository.findByNextEscalationTimeBeforeAndWorkflowStatus(now, status);

            for (WorkflowEntity entity : overdue) {
                entity.setEscalationLevel(entity.getEscalationLevel() + 1);
                entity.setNextEscalationTime(now.plusSeconds(AUTO_ESCALATION_BACKOFF_MINUTES * 60));
                entity.setRemarks("Auto-escalated: SLA escalation deadline breached.");
                entity.setWorkflowStatus(WorkflowStatus.ESCALATED);
                WorkflowEntity saved = repository.save(entity);

                publishEscalated(saved);
                log.info("Auto-escalated workflow {} to level {}", saved.getId(), saved.getEscalationLevel());
            }
        }
    }

    private void publishEscalated(WorkflowEntity saved) {
        producer.publishWorkflowEscalated(new WorkflowEscalatedEvent(UUID.randomUUID(), saved.getId(),
                                                                     saved.getIncidentId(),
                                                                     saved.getEscalationLevel(),
                                                                     saved.getAssignedTo(), saved.getAssignedTeam(),
                                                                     toLocalDateTime(Instant.now()),
                                                                     saved.getRemarks()));
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
