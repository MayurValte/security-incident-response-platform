package com.sirp.incident.incident.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sirp.common.enums.IncidentPriority;
import com.sirp.common.enums.IncidentSeverity;
import com.sirp.common.events.IncidentAssignedEvent;
import com.sirp.common.events.IncidentClosedEvent;
import com.sirp.common.events.IncidentCreatedEvent;
import com.sirp.common.events.IncidentResolvedEvent;
import com.sirp.incident.exception.AttachmentNotFoundException;
import com.sirp.incident.exception.IncidentNotFoundException;
import com.sirp.incident.exception.InvalidStatusTransitionException;
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
import com.sirp.incident.incident.entity.IncidentAttachment;
import com.sirp.incident.incident.entity.IncidentComment;
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
import com.sirp.incident.kafka.producer.IncidentEventProducer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class IncidentServiceImplTest {

    @Mock
    private IncidentRepository incidentRepository;
    @Mock
    private IncidentMapper incidentMapper;
    @Mock
    private IncidentNumberGenerator generator;
    @Mock
    private IncidentStatusValidator validator;
    @Mock
    private IncidentHistoryRepository historyRepository;
    @Mock
    private IncidentAssignmentRepository assignmentRepository;
    @Mock
    private IncidentSlaRepository slaRepository;
    @Mock
    private SlaCalculator slaCalculator;
    @Mock
    private IncidentEventProducer producer;
    @Mock
    private AssignmentValidator assignmentValidator;
    @Mock
    private IncidentCommentRepository commentRepository;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private IncidentAttachmentRepository attachmentRepository;
    @Mock
    private AttachmentMapper attachmentMapper;

    @InjectMocks
    private IncidentServiceImpl incidentService;

    @TempDir
    Path tempDir;

    private UUID incidentId;
    private UUID actorId;
    private Incident incident;

    @BeforeEach
    void setUp() {
        incidentId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        incident = Incident.builder()
            .id(incidentId)
            .incidentNumber("INC-2026-ABCD1234")
            .title("Prod outage")
            .description("Something is down")
            .status(IncidentStatus.OPEN)
            .severity(IncidentSeverity.HIGH)
            .priority(IncidentPriority.P1)
            .createdBy(actorId)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        ReflectionTestUtils.setField(incidentService, "attachmentStorageDir", tempDir.toString());
    }

    @Test
    void createIncidentGeneratesNumberSetsOpenStatusAndPublishesEvent() {
        CreateIncidentRequest request = new CreateIncidentRequest("Prod outage", "Something is down",
            IncidentSeverity.HIGH, IncidentPriority.P1, null);
        Incident mappedEntity = Incident.builder().title("Prod outage").description("Something is down")
            .severity(IncidentSeverity.HIGH).priority(IncidentPriority.P1).build();
        IncidentResponse response = new IncidentResponse(incidentId, "INC-2026-ABCD1234", "Prod outage",
            "Something is down", IncidentStatus.OPEN, IncidentSeverity.HIGH, IncidentPriority.P1, actorId, null,
            null, Instant.now(), Instant.now(), null, null, List.of());

        when(incidentMapper.toEntity(request)).thenReturn(mappedEntity);
        when(generator.generate()).thenReturn("INC-2026-ABCD1234");
        when(incidentRepository.save(any(Incident.class))).thenReturn(incident);
        when(slaCalculator.calculate(IncidentPriority.P1)).thenReturn(Instant.now().plusSeconds(3600));
        when(incidentMapper.toResponse(incident)).thenReturn(response);

        IncidentResponse result = incidentService.createIncident(request, actorId);

        assertThat(result).isEqualTo(response);
        assertThat(mappedEntity.getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(mappedEntity.getIncidentNumber()).isEqualTo("INC-2026-ABCD1234");
        assertThat(mappedEntity.getCreatedBy()).isEqualTo(actorId);

        ArgumentCaptor<IncidentSla> slaCaptor = ArgumentCaptor.forClass(IncidentSla.class);
        verify(slaRepository).save(slaCaptor.capture());
        assertThat(slaCaptor.getValue().getIncidentId()).isEqualTo(incidentId);
        assertThat(slaCaptor.getValue().getBreached()).isFalse();

        ArgumentCaptor<IncidentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(IncidentCreatedEvent.class);
        verify(producer).publishCreated(eventCaptor.capture());
        assertThat(eventCaptor.getValue().incidentId()).isEqualTo(incidentId);
    }

    @Nested
    class GetIncident {

        @Test
        void returnsMappedIncidentWhenFound() {
            IncidentResponse response = new IncidentResponse(incidentId, "INC-2026-ABCD1234", "Prod outage",
                "Something is down", IncidentStatus.OPEN, IncidentSeverity.HIGH, IncidentPriority.P1, actorId,
                null, null, Instant.now(), Instant.now(), null, null, List.of());
            when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
            when(incidentMapper.toResponse(incident)).thenReturn(response);

            assertThat(incidentService.getIncident(incidentId)).isEqualTo(response);
        }

        @Test
        void throwsIncidentNotFoundWhenMissing() {
            when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> incidentService.getIncident(incidentId))
                .isInstanceOf(IncidentNotFoundException.class);
        }
    }

    @Nested
    class SearchIncidents {

        @Test
        void mapsFilterStringsCaseInsensitivelyAndReturnsPagedSummaries() {
            IncidentSummaryResponse summary = new IncidentSummaryResponse(incidentId, "INC-2026-ABCD1234",
                "Prod outage", IncidentStatus.OPEN, IncidentSeverity.HIGH, IncidentPriority.P1, null,
                Instant.now());
            Page<Incident> page = new PageImpl<>(List.of(incident));
            when(incidentRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Incident>>any(),
                any(org.springframework.data.domain.Pageable.class))).thenReturn(page);
            when(incidentMapper.toSummary(incident)).thenReturn(summary);

            IncidentPageResponse result = incidentService.searchIncidents(0, 20, "open", "high", "p1");

            assertThat(result.content()).containsExactly(summary);
            assertThat(result.totalElements()).isEqualTo(1);
        }

        @Test
        void throwsIllegalArgumentForUnknownStatusValue() {
            assertThatThrownBy(() -> incidentService.searchIncidents(0, 20, "not_a_status", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class UpdateIncident {

        @Test
        void overwritesTitleAndDescription() {
            UpdateIncidentRequest request = new UpdateIncidentRequest("New title", "New description");
            IncidentResponse response = new IncidentResponse(incidentId, "INC-2026-ABCD1234", "New title",
                "New description", IncidentStatus.OPEN, IncidentSeverity.HIGH, IncidentPriority.P1, actorId, null,
                null, Instant.now(), Instant.now(), null, null, List.of());
            when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
            when(incidentRepository.save(incident)).thenReturn(incident);
            when(incidentMapper.toResponse(incident)).thenReturn(response);

            IncidentResponse result = incidentService.updateIncident(incidentId, request);

            assertThat(result).isEqualTo(response);
            assertThat(incident.getTitle()).isEqualTo("New title");
            assertThat(incident.getDescription()).isEqualTo("New description");
        }

        @Test
        void throwsWhenIncidentMissing() {
            UpdateIncidentRequest request = new UpdateIncidentRequest("New title", "New description");
            when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> incidentService.updateIncident(incidentId, request))
                .isInstanceOf(IncidentNotFoundException.class);
        }
    }

    @Nested
    class AssignIncident {

        @Test
        void movesToAcknowledgedRecordsHistoryAndPublishesEvent() {
            UUID assignee = UUID.randomUUID();
            AssignIncidentRequest request = new AssignIncidentRequest(assignee);
            IncidentResponse response = new IncidentResponse(incidentId, "INC-2026-ABCD1234", "Prod outage",
                "Something is down", IncidentStatus.ACKNOWLEDGED, IncidentSeverity.HIGH, IncidentPriority.P1,
                actorId, assignee, null, Instant.now(), Instant.now(), null, null, List.of());
            when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
            when(incidentRepository.save(incident)).thenReturn(incident);
            when(incidentMapper.toResponse(incident)).thenReturn(response);

            IncidentResponse result = incidentService.assignIncident(incidentId, request, actorId);

            assertThat(result).isEqualTo(response);
            assertThat(incident.getStatus()).isEqualTo(IncidentStatus.ACKNOWLEDGED);
            assertThat(incident.getAssignedTo()).isEqualTo(assignee);
            verify(validator).validate(IncidentStatus.OPEN, IncidentStatus.ACKNOWLEDGED);
            verify(assignmentValidator).validate(assignee);
            verify(historyRepository).save(any());
            verify(assignmentRepository).save(any());

            ArgumentCaptor<IncidentAssignedEvent> eventCaptor = ArgumentCaptor.forClass(IncidentAssignedEvent.class);
            verify(producer).publishAssigned(eventCaptor.capture());
            assertThat(eventCaptor.getValue().assignedTo()).isEqualTo(assignee);
            assertThat(eventCaptor.getValue().assignedBy()).isEqualTo(actorId);
        }

        @Test
        void fallsBackToCreatedByWhenActorIdNull() {
            UUID assignee = UUID.randomUUID();
            AssignIncidentRequest request = new AssignIncidentRequest(assignee);
            when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
            when(incidentRepository.save(incident)).thenReturn(incident);
            when(incidentMapper.toResponse(incident)).thenReturn(
                new IncidentResponse(incidentId, "INC-2026-ABCD1234", "Prod outage", "Something is down",
                    IncidentStatus.ACKNOWLEDGED, IncidentSeverity.HIGH, IncidentPriority.P1, actorId, assignee,
                    null, Instant.now(), Instant.now(), null, null, List.of()));

            incidentService.assignIncident(incidentId, request, null);

            ArgumentCaptor<IncidentAssignedEvent> eventCaptor = ArgumentCaptor.forClass(IncidentAssignedEvent.class);
            verify(producer).publishAssigned(eventCaptor.capture());
            assertThat(eventCaptor.getValue().assignedBy()).isEqualTo(incident.getCreatedBy());
        }

        @Test
        void propagatesInvalidTransitionWithoutTouchingAssignmentValidator() {
            AssignIncidentRequest request = new AssignIncidentRequest(UUID.randomUUID());
            incident.setStatus(IncidentStatus.CLOSED);
            when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
            org.mockito.Mockito.doThrow(new InvalidStatusTransitionException("CLOSED", "ACKNOWLEDGED"))
                .when(validator).validate(IncidentStatus.CLOSED, IncidentStatus.ACKNOWLEDGED);

            assertThatThrownBy(() -> incidentService.assignIncident(incidentId, request, actorId))
                .isInstanceOf(InvalidStatusTransitionException.class);

            verify(assignmentValidator, never()).validate(any());
            verify(incidentRepository, never()).save(any());
        }
    }

    @Nested
    class ResolveIncident {

        @Test
        void marksSlaBreachedWhenResolvedAfterTargetTime() {
            ResolveIncidentRequest request = new ResolveIncidentRequest("Fixed the root cause");
            incident.setStatus(IncidentStatus.IN_PROGRESS);
            incident.setAssignedTo(actorId);
            IncidentSla sla = IncidentSla.builder().incidentId(incidentId)
                .targetResolutionTime(Instant.now().minusSeconds(60)).breached(false).build();
            when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
            when(incidentRepository.save(incident)).thenReturn(incident);
            when(slaRepository.findByIncidentId(incidentId)).thenReturn(Optional.of(sla));
            when(incidentMapper.toResponse(incident)).thenReturn(
                new IncidentResponse(incidentId, "INC-2026-ABCD1234", "Prod outage", "Something is down",
                    IncidentStatus.RESOLVED, IncidentSeverity.HIGH, IncidentPriority.P1, actorId, actorId, null,
                    Instant.now(), Instant.now(), Instant.now(), null, List.of()));

            incidentService.resolveIncident(incidentId, request, actorId);

            assertThat(incident.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
            assertThat(incident.getResolvedAt()).isNotNull();
            assertThat(sla.getBreached()).isTrue();
            assertThat(sla.getBreachedAt()).isNotNull();
            verify(slaRepository).save(sla);

            ArgumentCaptor<IncidentResolvedEvent> eventCaptor = ArgumentCaptor.forClass(IncidentResolvedEvent.class);
            verify(producer).publishResolved(eventCaptor.capture());
            assertThat(eventCaptor.getValue().resolvedBy()).isEqualTo(actorId);
        }

        @Test
        void leavesSlaUnbreachedWhenResolvedBeforeTargetTime() {
            ResolveIncidentRequest request = new ResolveIncidentRequest("Fixed the root cause");
            incident.setStatus(IncidentStatus.IN_PROGRESS);
            IncidentSla sla = IncidentSla.builder().incidentId(incidentId)
                .targetResolutionTime(Instant.now().plusSeconds(3600)).breached(false).build();
            when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
            when(incidentRepository.save(incident)).thenReturn(incident);
            when(slaRepository.findByIncidentId(incidentId)).thenReturn(Optional.of(sla));
            when(incidentMapper.toResponse(incident)).thenReturn(
                new IncidentResponse(incidentId, "INC-2026-ABCD1234", "Prod outage", "Something is down",
                    IncidentStatus.RESOLVED, IncidentSeverity.HIGH, IncidentPriority.P1, actorId, null, null,
                    Instant.now(), Instant.now(), Instant.now(), null, List.of()));

            incidentService.resolveIncident(incidentId, request, actorId);

            assertThat(sla.getBreached()).isFalse();
            assertThat(sla.getBreachedAt()).isNull();
        }

        @Test
        void throwsIllegalStateWhenSlaRecordMissing() {
            ResolveIncidentRequest request = new ResolveIncidentRequest("Fixed the root cause");
            incident.setStatus(IncidentStatus.IN_PROGRESS);
            when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
            when(incidentRepository.save(incident)).thenReturn(incident);
            when(slaRepository.findByIncidentId(incidentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> incidentService.resolveIncident(incidentId, request, actorId))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void closeIncidentSetsClosedStatusAndPublishesEvent() {
        incident.setStatus(IncidentStatus.RESOLVED);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident)).thenReturn(incident);
        when(incidentMapper.toResponse(incident)).thenReturn(
            new IncidentResponse(incidentId, "INC-2026-ABCD1234", "Prod outage", "Something is down",
                IncidentStatus.CLOSED, IncidentSeverity.HIGH, IncidentPriority.P1, actorId, null, null,
                Instant.now(), Instant.now(), null, Instant.now(), List.of()));

        incidentService.closeIncident(incidentId, actorId);

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CLOSED);
        assertThat(incident.getClosedAt()).isNotNull();
        ArgumentCaptor<IncidentClosedEvent> eventCaptor = ArgumentCaptor.forClass(IncidentClosedEvent.class);
        verify(producer).publishClosed(eventCaptor.capture());
        assertThat(eventCaptor.getValue().closedBy()).isEqualTo(actorId);
    }

    @Test
    void startIncidentSetsInProgressAndDoesNotPublishAnEvent() {
        incident.setStatus(IncidentStatus.ACKNOWLEDGED);
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(incident)).thenReturn(incident);
        when(incidentMapper.toResponse(incident)).thenReturn(
            new IncidentResponse(incidentId, "INC-2026-ABCD1234", "Prod outage", "Something is down",
                IncidentStatus.IN_PROGRESS, IncidentSeverity.HIGH, IncidentPriority.P1, actorId, null, null,
                Instant.now(), Instant.now(), null, null, List.of()));

        incidentService.startIncident(incidentId, actorId);

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        org.mockito.Mockito.verifyNoInteractions(producer);
    }

    @Test
    void addCommentSavesAgainstParentIncidentAndReturnsMappedResponse() {
        AddCommentRequest request = new AddCommentRequest("Investigating now");
        IncidentComment saved = IncidentComment.builder().id(UUID.randomUUID()).incident(incident)
            .message("Investigating now").createdBy(actorId).createdAt(Instant.now()).build();
        CommentResponse response = new CommentResponse(saved.getId(), "Investigating now", actorId,
            saved.getCreatedAt());
        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(commentRepository.save(any(IncidentComment.class))).thenReturn(saved);
        when(commentMapper.toResponse(saved)).thenReturn(response);

        CommentResponse result = incidentService.addComment(incidentId, request, actorId);

        assertThat(result).isEqualTo(response);
        ArgumentCaptor<IncidentComment> captor = ArgumentCaptor.forClass(IncidentComment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getIncident()).isEqualTo(incident);
        assertThat(captor.getValue().getMessage()).isEqualTo("Investigating now");
    }

    @Nested
    class UploadAttachment {

        @Test
        void storesFileUnderIncidentDirectoryAndSavesMetadata() {
            MockMultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf",
                "content".getBytes());
            IncidentAttachment saved = IncidentAttachment.builder().id(UUID.randomUUID()).incidentId(incidentId)
                .fileName("report.pdf").contentType("application/pdf").fileSize(7L).uploadedBy(actorId)
                .uploadedAt(Instant.now()).build();
            AttachmentResponse response = new AttachmentResponse(saved.getId(), "report.pdf", "application/pdf",
                7L, actorId, saved.getUploadedAt());
            when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
            when(attachmentRepository.save(any(IncidentAttachment.class))).thenReturn(saved);
            when(attachmentMapper.toResponse(saved)).thenReturn(response);

            AttachmentResponse result = incidentService.uploadAttachment(incidentId, file, actorId);

            assertThat(result).isEqualTo(response);
            ArgumentCaptor<IncidentAttachment> captor = ArgumentCaptor.forClass(IncidentAttachment.class);
            verify(attachmentRepository).save(captor.capture());
            assertThat(captor.getValue().getFileName()).isEqualTo("report.pdf");
            assertThat(Path.of(captor.getValue().getStorageUrl())).exists();
            assertThat(Path.of(captor.getValue().getStorageUrl()).getParent().getFileName().toString())
                .isEqualTo(incidentId.toString());
        }

        @Test
        void stripsPathTraversalSegmentsFromClientSuppliedFileName() {
            MockMultipartFile file = new MockMultipartFile("file", "../../etc/passwd", "text/plain",
                "content".getBytes());
            when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
            when(attachmentRepository.save(any(IncidentAttachment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(attachmentMapper.toResponse(any())).thenReturn(
                new AttachmentResponse(UUID.randomUUID(), "passwd", "text/plain", 7L, actorId, Instant.now()));

            incidentService.uploadAttachment(incidentId, file, actorId);

            ArgumentCaptor<IncidentAttachment> captor = ArgumentCaptor.forClass(IncidentAttachment.class);
            verify(attachmentRepository).save(captor.capture());
            assertThat(captor.getValue().getFileName()).isEqualTo("passwd");
            assertThat(Path.of(captor.getValue().getStorageUrl()))
                .startsWith(tempDir.resolve(incidentId.toString()));
        }

        @Test
        void throwsIllegalArgumentWhenFileEmpty() {
            MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
            when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

            assertThatThrownBy(() -> incidentService.uploadAttachment(incidentId, emptyFile, actorId))
                .isInstanceOf(IllegalArgumentException.class);

            verify(attachmentRepository, never()).save(any());
        }

        @Test
        void throwsIncidentNotFoundWhenIncidentMissing() {
            MockMultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf",
                "content".getBytes());
            when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> incidentService.uploadAttachment(incidentId, file, actorId))
                .isInstanceOf(IncidentNotFoundException.class);
        }
    }

    @Nested
    class ListAttachments {

        @Test
        void returnsMappedAttachmentsWhenIncidentExists() {
            when(incidentRepository.existsById(incidentId)).thenReturn(true);
            List<IncidentAttachment> attachments = List.of(
                IncidentAttachment.builder().id(UUID.randomUUID()).incidentId(incidentId).build());
            List<AttachmentResponse> responses = List.of(
                new AttachmentResponse(UUID.randomUUID(), "a.txt", "text/plain", 1L, actorId, Instant.now()));
            when(attachmentRepository.findByIncidentId(incidentId)).thenReturn(attachments);
            when(attachmentMapper.toResponseList(attachments)).thenReturn(responses);

            assertThat(incidentService.listAttachments(incidentId)).isEqualTo(responses);
        }

        @Test
        void throwsIncidentNotFoundWhenIncidentMissing() {
            when(incidentRepository.existsById(incidentId)).thenReturn(false);

            assertThatThrownBy(() -> incidentService.listAttachments(incidentId))
                .isInstanceOf(IncidentNotFoundException.class);
        }
    }

    @Nested
    class DownloadAttachment {

        @Test
        void returnsResourceWhenAttachmentBelongsToIncidentAndFileExists() throws Exception {
            UUID attachmentId = UUID.randomUUID();
            Path storedFile = tempDir.resolve("stored.txt");
            Files.writeString(storedFile, "hello");
            IncidentAttachment attachment = IncidentAttachment.builder().id(attachmentId).incidentId(incidentId)
                .fileName("report.txt").contentType("text/plain").storageUrl(storedFile.toString()).build();
            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

            AttachmentFile result = incidentService.downloadAttachment(incidentId, attachmentId);

            assertThat(result.fileName()).isEqualTo("report.txt");
            assertThat(result.resource().exists()).isTrue();
        }

        @Test
        void throwsAttachmentNotFoundWhenAttachmentBelongsToADifferentIncident() {
            UUID attachmentId = UUID.randomUUID();
            IncidentAttachment attachment = IncidentAttachment.builder().id(attachmentId)
                .incidentId(UUID.randomUUID()).fileName("report.txt").storageUrl(tempDir.resolve("x").toString())
                .build();
            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

            assertThatThrownBy(() -> incidentService.downloadAttachment(incidentId, attachmentId))
                .isInstanceOf(AttachmentNotFoundException.class);
        }

        @Test
        void throwsAttachmentNotFoundWhenFileMissingFromDisk() {
            UUID attachmentId = UUID.randomUUID();
            IncidentAttachment attachment = IncidentAttachment.builder().id(attachmentId).incidentId(incidentId)
                .fileName("report.txt").storageUrl(tempDir.resolve("does-not-exist.txt").toString()).build();
            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

            assertThatThrownBy(() -> incidentService.downloadAttachment(incidentId, attachmentId))
                .isInstanceOf(AttachmentNotFoundException.class);
        }

        @Test
        void throwsAttachmentNotFoundWhenAttachmentIdUnknown() {
            UUID attachmentId = UUID.randomUUID();
            when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> incidentService.downloadAttachment(incidentId, attachmentId))
                .isInstanceOf(AttachmentNotFoundException.class);
        }
    }
}
