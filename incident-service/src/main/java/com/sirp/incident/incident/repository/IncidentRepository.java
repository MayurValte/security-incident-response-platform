package com.sirp.incident.incident.repository;

import com.sirp.common.enums.IncidentPriority;
import com.sirp.common.enums.IncidentSeverity;
import com.sirp.incident.incident.entity.Incident;
import com.sirp.incident.incident.enums.IncidentStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID>, JpaSpecificationExecutor<Incident> {

    Optional<Incident> findByIncidentNumber(String incidentNumber);

    boolean existsByIncidentNumber(String incidentNumber);

    Page<Incident> findByStatus(IncidentStatus status, Pageable pageable);

    Page<Incident> findBySeverity(IncidentSeverity severity, Pageable pageable);

    Page<Incident> findByPriority(IncidentPriority priority, Pageable pageable);

    Page<Incident> findByAssignedTo(UUID assignedTo, Pageable pageable);
}