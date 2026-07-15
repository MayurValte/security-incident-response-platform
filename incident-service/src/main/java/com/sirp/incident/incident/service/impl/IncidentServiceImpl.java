package com.sirp.incident.incident.service.impl;

import com.sirp.common.enums.IncidentPriority;
import com.sirp.common.enums.IncidentSeverity;
import com.sirp.common.events.IncidentAssignedEvent;
import com.sirp.common.events.IncidentClosedEvent;
import com.sirp.common.events.IncidentCreatedEvent;
import com.sirp.common.events.IncidentResolvedEvent;
import com.sirp.incident.exception.AttachmentNotFoundException;
import com.sirp.incident.exception.IncidentNotFoundException;
import com.sirp.incident.incident.dto.request.AddCommentRequest;
import com.sirp.incident.incident.dto.request.AssignIncidentRequest;
import com.sirp.incident.incident.dto.request.CreateIncidentRequest;
import com.sirp.incident.incident.dto.request.ResolveIncidentRequest;
import com.sirp.incident.incident.dto.request.UpdateIncidentRequest;
import com.sirp.incident.incident.dto.response.AttachmentFile;
import com.sirp.incident.incident.dto.response.AttachmentResponse;
import com.sirp.incident.incident.dto.response.CommentResponse;
import com.sirp.incident.incident.dto.response.IncidentPageResponse;
import com.sirp.incident.incident.dto.response.IncidentResponse;
import com.sirp.incident.incident.dto.response.IncidentSummaryResponse;
import com.sirp.incident.incident.entity.Incident;
import com.sirp.incident.incident.entity.IncidentAssignment;
import com.sirp.incident.incident.entity.IncidentAttachment;
import com.sirp.incident.incident.entity.IncidentComment;
import com.sirp.incident.incident.entity.IncidentHistory;
import com.sirp.incident.incident.entity.IncidentSla;
import com.sirp.incident.incident.enums.IncidentStatus;
import com.sirp.incident.incident.helper.AssignmentValidator;
import com.sirp.incident.incident.helper.IncidentNumberGenerator;
import com.sirp.incident.incident.helper.IncidentStatusValidator;
import com.sirp.incident.incident.helper.SlaCalculator;
import com.sirp.incident.incident.mapper.AttachmentMapper;
import com.sirp.incident.incident.mapper.CommentMapper;
import com.sirp.incident.incident.mapper.IncidentMapper;
import com.sirp.incident.incident.repository.IncidentAssignmentRepository;
import com.sirp.incident.incident.repository.IncidentAttachmentRepository;
import com.sirp.incident.incident.repository.IncidentCommentRepository;
import com.sirp.incident.incident.repository.IncidentHistoryRepository;
import com.sirp.incident.incident.repository.IncidentRepository;
import com.sirp.incident.incident.repository.IncidentSlaRepository;
import com.sirp.incident.incident.service.IncidentService;
import com.sirp.incident.incident.specification.IncidentSpecification;
import com.sirp.incident.kafka.producer.IncidentEventProducer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final IncidentCommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final IncidentAttachmentRepository attachmentRepository;
    private final AttachmentMapper attachmentMapper;

    @Value("${incident.attachment.storage-dir}")
    private String attachmentStorageDir;

    @Override
    public IncidentResponse createIncident(CreateIncidentRequest request, UUID actorId) {
        Incident incident = incidentMapper.toEntity(request);
        incident.setIncidentNumber(generator.generate());
        incident.setStatus(IncidentStatus.OPEN);
        incident.setCreatedBy(actorId);
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
    public IncidentResponse assignIncident(UUID id, AssignIncidentRequest request, UUID actorId) {
        Incident incident = incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
        validator.validate(incident.getStatus(), IncidentStatus.ACKNOWLEDGED);
        assignmentValidator.validate(request.assignedTo());
        UUID effectiveActor = actorId != null ? actorId : incident.getCreatedBy();
        IncidentHistory history = IncidentHistory.builder()
                                                 .incident(incident)
                                                 .oldStatus(incident.getStatus())
                                                 .newStatus(IncidentStatus.ACKNOWLEDGED)
                                                 .changedBy(effectiveActor)
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
        producer.publishAssigned(new IncidentAssignedEvent(UUID.randomUUID(), incident.getId(),
                                                           incident.getIncidentNumber(), incident.getTitle(),
                                                           incident.getPriority(), incident.getSeverity(),
                                                           request.assignedTo(), effectiveActor,
                                                           Instant.now()));
        return incidentMapper.toResponse(saved);
    }

    @Override
    public IncidentResponse resolveIncident(UUID id, ResolveIncidentRequest request, UUID actorId) {
        Instant now = Instant.now();
        Incident incident = incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
        validator.validate(incident.getStatus(), IncidentStatus.RESOLVED);
        UUID effectiveActor = actorId != null ? actorId : incident.getAssignedTo();
        IncidentHistory history = IncidentHistory.builder()
                                                 .incident(incident)
                                                 .oldStatus(incident.getStatus())
                                                 .newStatus(IncidentStatus.RESOLVED)
                                                 .changedBy(effectiveActor)
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
            new IncidentResolvedEvent(UUID.randomUUID(), incident.getId(), incident.getIncidentNumber(),
                                      incident.getTitle(), incident.getPriority(), incident.getSeverity(),
                                      effectiveActor, now));
        return incidentMapper.toResponse(saved);
    }

    @Override
    public IncidentResponse closeIncident(UUID id, UUID actorId) {
        Incident incident = incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
        validator.validate(incident.getStatus(), IncidentStatus.CLOSED);
        UUID effectiveActor = actorId != null ? actorId : incident.getAssignedTo();
        IncidentHistory history = IncidentHistory.builder()
                                                 .incident(incident)
                                                 .oldStatus(incident.getStatus())
                                                 .newStatus(IncidentStatus.CLOSED)
                                                 .changedBy(effectiveActor)
                                                 .changedAt(Instant.now())
                                                 .build();
        historyRepository.save(history);
        incident.setStatus(IncidentStatus.CLOSED);
        incident.setClosedAt(Instant.now());
        incident.setUpdatedAt(Instant.now());
        Incident saved = incidentRepository.save(incident);
        producer.publishClosed(
            new IncidentClosedEvent(UUID.randomUUID(), incident.getId(), incident.getIncidentNumber(),
                                    incident.getTitle(), incident.getPriority(), incident.getSeverity(),
                                    effectiveActor, Instant.now()));
        return incidentMapper.toResponse(saved);
    }

    @Override
    public IncidentResponse startIncident(UUID id, UUID actorId) {
        Incident incident = incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
        validator.validate(incident.getStatus(), IncidentStatus.IN_PROGRESS);
        UUID effectiveActor = actorId != null ? actorId : incident.getAssignedTo();
        IncidentHistory history = IncidentHistory.builder()
                                                 .incident(incident)
                                                 .oldStatus(incident.getStatus())
                                                 .newStatus(IncidentStatus.IN_PROGRESS)
                                                 .changedBy(effectiveActor)
                                                 .changedAt(Instant.now())
                                                 .build();
        historyRepository.save(history);
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.setUpdatedAt(Instant.now());
        Incident saved = incidentRepository.save(incident);
        return incidentMapper.toResponse(saved);
    }

    @Override
    public CommentResponse addComment(UUID id, AddCommentRequest request, UUID actorId) {
        Incident incident = incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
        IncidentComment comment = IncidentComment.builder()
                                                 .incident(incident)
                                                 .message(request.message())
                                                 .createdBy(actorId)
                                                 .createdAt(Instant.now())
                                                 .build();
        IncidentComment saved = commentRepository.save(comment);
        return commentMapper.toResponse(saved);
    }

    @Override
    public AttachmentResponse uploadAttachment(UUID id, MultipartFile file, UUID actorId) {
        Incident incident = incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Attachment file must not be empty");
        }

        // Original filename comes from the client and is untrusted - take only the
        // final path segment (discarding any directory components it might contain)
        // so it can never be used to escape the per-incident storage directory below.
        String rawName = file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename();
        String safeName = Path.of(rawName).getFileName().toString();
        String storedFileName = UUID.randomUUID() + "_" + safeName;

        try {
            Path incidentDir = Path.of(attachmentStorageDir, incident.getId().toString()).normalize();
            Files.createDirectories(incidentDir);
            Path target = incidentDir.resolve(storedFileName);
            file.transferTo(target);

            IncidentAttachment attachment = IncidentAttachment.builder()
                                                              .incidentId(incident.getId())
                                                              .fileName(safeName)
                                                              .contentType(file.getContentType())
                                                              .fileSize(file.getSize())
                                                              .storageUrl(target.toString())
                                                              .uploadedBy(actorId)
                                                              .uploadedAt(Instant.now())
                                                              .build();
            IncidentAttachment saved = attachmentRepository.save(attachment);
            return attachmentMapper.toResponse(saved);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store attachment for incident " + id, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponse> listAttachments(UUID id) {
        if (!incidentRepository.existsById(id)) {
            throw new IncidentNotFoundException(id);
        }
        return attachmentMapper.toResponseList(attachmentRepository.findByIncidentId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentFile downloadAttachment(UUID id, UUID attachmentId) {
        IncidentAttachment attachment = attachmentRepository.findById(attachmentId)
                                                            .filter(a -> id.equals(a.getIncidentId()))
                                                            .orElseThrow(
                                                                () -> new AttachmentNotFoundException(attachmentId));
        try {
            Path path = Path.of(attachment.getStorageUrl());
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new AttachmentNotFoundException(attachmentId);
            }
            return new AttachmentFile(resource, attachment.getFileName(), attachment.getContentType());
        } catch (MalformedURLException e) {
            throw new AttachmentNotFoundException(attachmentId);
        }
    }
}