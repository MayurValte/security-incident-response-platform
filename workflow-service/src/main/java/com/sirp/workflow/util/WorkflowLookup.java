package com.sirp.workflow.util;

import com.sirp.workflow.entity.WorkflowEntity;
import com.sirp.workflow.exception.WorkflowNotFoundException;
import com.sirp.workflow.repository.WorkflowRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public final class WorkflowLookup {

    private WorkflowLookup() {
    }

    public static WorkflowEntity findOrThrow(WorkflowRepository repository, UUID workflowId) {
        return repository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException("Workflow not found: " + workflowId));
    }

    public static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
