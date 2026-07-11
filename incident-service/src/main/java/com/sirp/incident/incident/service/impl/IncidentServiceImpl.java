package com.sirp.incident.incident.service.impl;

import com.sirp.common.enums.IncidentPriority;
import com.sirp.common.enums.IncidentSeverity;
import com.sirp.common.events.IncidentAssignedEvent;
import com.sirp.common.events.IncidentClosedEvent;
import com.sirp.common.events.IncidentCreatedEvent;
import com.sirp.common.events.IncidentResolvedEvent;
import com.sirp.incident.exception.IncidentNotFoundException;
import com.sirp.incident.incident.dto.request.AssignIncidentRequest;
import com.sirp.incident.incident.dto.request.CreateIncidentRequest;
import com.sirp.incident.incident.dto.request.ResolveIncidentRequest;
import com.sirp.incident.incident.dto.request.UpdateIncidentRequest;
import com.sirp.incident.incident.dto.response.IncidentPageResponse;
import com.sirp.incident.incident.dto.response.IncidentResponse;
import com.sirp.incident.incident.dto.response.IncidentSummaryResponse;
import com.sirp.incident.incident.entity.Incident;
import com.sirp.incident.incident.entity.IncidentAssignment;
import com.sirp.incident.incident.entity.IncidentHistory;
import com.sirp.incident.incident.entity.IncidentSla;
import com.sirp.incident.incident.enums.IncidentStatus;
import com.sirp.incident.incident.helper.AssignmentValidator;
import com.sirp.incident.incident.helper.IncidentNumberGenerator;
import com.sirp.incident.incident.helper.IncidentStatusValidator;
import com.sirp.incident.incident.helper.SlaCalculator;
import com.sirp.incident.incident.mapper.IncidentMapper;
import com.sirp.incident.incident.repository.IncidentAssignmentRepository;
import com.sirp.incident.incident.repository.IncidentHistoryRepository;
import com.sirp.incident.incident.repository.IncidentRepository;
import com.sirp.incident.incident.repository.IncidentSlaRepository;
import com.sirp.incident.incident.service.IncidentService;
import com.sirp.incident.incident.specification.IncidentSpecification;
import com.sirp.incident.kafka.producer.IncidentEventProducer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentMapper incidentMapper;
    private final IncidentNumberGenerator generator;
    private final IncidentStatusValidator validator;
    private final IncidentHistoryRepository historyRepository;
    private final IncidentAssignmentRepository assignmentRepository;
    private final IncidentSlaRepository slaRepository;
    private final SlaCalculator slaCalculator;
    private final IncidentEventProducer producer;
    private final AssignmentValidator assignmentValidator;

    @Override
    public IncidentResponse createIncident(CreateIncidentRequest request) {
        Incident incident = incidentMapper.toEntity(request);
        incident.setIncidentNumber(generator.generate());
        incident.setStatus(IncidentStatus.OPEN);
        incident.setCreatedBy(UUID.fromString("5c7858ad-3bbd-4fbd-8ec0-cfb72ffce8f3"));
        incident.setCreatedAt(Instant.now());
        incident.setUpdatedAt(Instant.now());
        Incident saved = incidentRepository.save(incident);
        IncidentSla sla = IncidentSla.builder().incidentId(saved.getId()).targetResolutionTime(
            slaCalculator.calculate(saved.getPriority())).breached(false).build();
        slaRepository.save(sla);
        producer.publishCreated(
            new IncidentCreatedEvent(UUID.randomUUID(), saved.getId(), saved.getIncidentNumber(), saved.getTitle(),
                                     saved.getDescription(), saved.getPriority(), saved.getSeverity(),
                                     saved.getCreatedBy(), Instant.now()));
        return incidentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentResponse getIncident(UUID id) {
        Incident incident = incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
        return incidentMapper.toResponse(incident);
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentPageResponse searchIncidents(Integer page,
        Integer size,
        String status,
        String severity,
        String priority) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        IncidentStatus incidentStatus = status == null ? null : IncidentStatus.valueOf(status.toUpperCase());
        IncidentSeverity incidentSeverity = severity == null ? null : IncidentSeverity.valueOf(severity.toUpperCase());
        IncidentPriority incidentPriority = priority == null ? null : IncidentPriority.valueOf(priority.toUpperCase());
        Specification<Incident> specification = Specification.allOf(IncidentSpecification.hasStatus(incidentStatus),
                                                                    IncidentSpecification.hasSeverity(incidentSeverity),
                                                                    IncidentSpecification.hasPriority(
                                                                        incidentPriority));
        Page<Incident> incidents = incidentRepository.findAll(specification, pageable);
        List<IncidentSummaryResponse> content = incidents.stream().map(incidentMapper::toSummary).toList();
        return new IncidentPageResponse(content, incidents.getNumber(), incidents.getSize(),
                                        incidents.getTotalElements(), incidents.getTotalPages(), incidents.isFirst(),
                                        incidents.isLast());
    }

    @Override
    public IncidentResponse updateIncident(UUID id, UpdateIncidentRequest request) {
        Incident incident = incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
        incident.setTitle(request.title());
        incident.setDescription(request.description());
        incident.setUpdatedAt(Instant.now());
        Incident saved = incidentRepository.save(incident);
        return incidentMapper.toResponse(saved);
    }

    @Override
    public IncidentResponse assignIncident(UUID id, AssignIncidentRequest request) {
        Incident incident = incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
        validator.validate(incident.getStatus(), IncidentStatus.ACKNOWLEDGED);
        IncidentHistory history = IncidentHistory.builder()
                                                 .incident(incident)
                                                 .oldStatus(incident.getStatus())
                                                 .newStatus(IncidentStatus.ACKNOWLEDGED)
                                                 .changedBy(incident.getCreatedBy())
                                                 .changedAt(Instant.now())
                                                 .build();
        historyRepository.save(history);
        incident.setAssignedTo(request.assignedTo());
        incident.setStatus(IncidentStatus.ACKNOWLEDGED);
        incident.setUpdatedAt(Instant.now());
        IncidentAssignment assignment = IncidentAssignment.builder().incidentId(incident.getId()).assignedTo(
            request.assignedTo()).assignedAt(Instant.now()).build();
        assignmentRepository.save(assignment);
        Incident saved = incidentRepository.save(incident);
        producer.publishAssigned(new IncidentAssignedEvent(UUID.randomUUID(), incident.getId(), request.assignedTo(),
                                                           incident.getCreatedBy(), Instant.now()));
        return incidentMapper.toResponse(saved);
    }

    @Override
    public IncidentResponse resolveIncident(UUID id, ResolveIncidentRequest request) {
        Instant now = Instant.now();
        Incident incident = incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
        validator.validate(incident.getStatus(), IncidentStatus.RESOLVED);
        IncidentHistory history = IncidentHistory.builder()
                                                 .incident(incident)
                                                 .oldStatus(incident.getStatus())
                                                 .newStatus(IncidentStatus.RESOLVED)
                                                 .changedBy(incident.getAssignedTo())
                                                 .changedAt(now)
                                                 .build();
        historyRepository.save(history);
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(now);
        incident.setUpdatedAt(now);
        Incident saved = incidentRepository.save(incident);
        IncidentSla sla = slaRepository.findByIncidentId(incident.getId()).orElseThrow(
            () -> new IllegalStateException("SLA not found for incident " + incident.getId()));
        if (now.isAfter(sla.getTargetResolutionTime())) {
            sla.setBreached(true);
            sla.setBreachedAt(now);
        }
        slaRepository.save(sla);
        producer.publishResolved(
            new IncidentResolvedEvent(UUID.randomUUID(), incident.getId(), incident.getCreatedBy(), now));
        return incidentMapper.toResponse(saved);
    }

    @Override
    public IncidentResponse closeIncident(UUID id) {
        Incident incident = incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
        validator.validate(incident.getStatus(), IncidentStatus.CLOSED);
        IncidentHistory history = IncidentHistory.builder()
                                                 .incident(incident)
                                                 .oldStatus(incident.getStatus())
                                                 .newStatus(IncidentStatus.CLOSED)
                                                 .changedBy(incident.getAssignedTo())
                                                 .changedAt(Instant.now())
                                                 .build();
        historyRepository.save(history);
        incident.setStatus(IncidentStatus.CLOSED);
        incident.setClosedAt(Instant.now());
        incident.setUpdatedAt(Instant.now());
        Incident saved = incidentRepository.save(incident);
        producer.publishClosed(
            new IncidentClosedEvent(UUID.randomUUID(), incident.getId(), incident.getCreatedBy(), Instant.now()));
        return incidentMapper.toResponse(saved);
    }

    @Override
    public IncidentResponse startIncident(UUID id) {
        Incident incident = incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
        validator.validate(incident.getStatus(), IncidentStatus.IN_PROGRESS);
        IncidentHistory history = IncidentHistory.builder()
                                                 .incident(incident)
                                                 .oldStatus(incident.getStatus())
                                                 .newStatus(IncidentStatus.IN_PROGRESS)
                                                 .changedBy(incident.getAssignedTo())
                                                 .changedAt(Instant.now())
                                                 .build();
        historyRepository.save(history);
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.setUpdatedAt(Instant.now());
        Incident saved = incidentRepository.save(incident);
        return incidentMapper.toResponse(saved);
    }
}