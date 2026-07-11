package com.sirp.workflow.repository;

import com.sirp.common.enums.WorkflowStatus;
import com.sirp.workflow.entity.WorkflowEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowRepository extends JpaRepository<WorkflowEntity, UUID> {

    Optional<WorkflowEntity> findByIncidentId(UUID incidentId);

    List<WorkflowEntity> findByAssignedTo(UUID assignedTo);

    List<WorkflowEntity> findByAssignedTeam(UUID assignedTeam);

    List<WorkflowEntity> findByWorkflowStatus(WorkflowStatus workflowStatus);

    List<WorkflowEntity> findByWorkflowStatusAndAssignedTeam(
        WorkflowStatus workflowStatus,
        UUID assignedTeam
                                                            );

    List<WorkflowEntity> findByWorkflowStatusAndAssignedTo(
        WorkflowStatus workflowStatus,
        UUID assignedTo
                                                          );

    List<WorkflowEntity> findByNextEscalationTimeBeforeAndWorkflowStatus(
        Instant currentTime,
        WorkflowStatus workflowStatus
                                                                        );

    boolean existsByIncidentId(UUID incidentId);

}