package com.sirp.incident.incident.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirp.common.dto.PageResponse;
import com.sirp.common.enums.IncidentPriority;
import com.sirp.common.enums.IncidentSeverity;
import com.sirp.incident.exception.GlobalExceptionHandler;
import com.sirp.incident.exception.IncidentNotFoundException;
import com.sirp.incident.exception.InvalidStatusTransitionException;
import com.sirp.incident.incident.dto.request.AddCommentRequest;
import com.sirp.incident.incident.dto.request.AssignIncidentRequest;
import com.sirp.incident.incident.dto.request.CreateIncidentRequest;
import com.sirp.incident.incident.dto.request.ResolveIncidentRequest;
import com.sirp.incident.incident.dto.request.UpdateIncidentRequest;
import com.sirp.incident.incident.dto.response.AttachmentResponse;
import com.sirp.incident.incident.dto.response.CommentResponse;
import com.sirp.incident.incident.dto.response.IncidentResponse;
import com.sirp.incident.incident.dto.response.IncidentSummaryResponse;
import com.sirp.incident.incident.enums.IncidentStatus;
import com.sirp.incident.incident.service.IncidentService;
import com.sirp.security.model.JwtUser;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class IncidentControllerTest {

    @Mock
    private IncidentService incidentService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private UUID actorId;

    @BeforeEach
    void setUp() {
        IncidentController controller = new IncidentController(incidentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(new LocalValidatorFactoryBean())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();

        actorId = UUID.randomUUID();
        JwtUser principal = new JwtUser(actorId, "jdoe@sirp.local", "ENGINEER", null);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(principal, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private IncidentResponse sampleResponse(UUID id, IncidentStatus status) {
        return new IncidentResponse(id, "INC-2026-ABCD1234", "Prod outage", "Something is down", status,
            IncidentSeverity.HIGH, IncidentPriority.P1, actorId, null, null, Instant.now(), Instant.now(), null,
            null, List.of());
    }

    @Test
    void createIncident_returns201WithActorFromPrincipal() throws Exception {
        CreateIncidentRequest request = new CreateIncidentRequest("Prod outage", "Something is down",
            IncidentSeverity.HIGH, IncidentPriority.P1, null);
        UUID id = UUID.randomUUID();
        when(incidentService.createIncident(eq(request), eq(actorId))).thenReturn(sampleResponse(id,
            IncidentStatus.OPEN));

        mockMvc.perform(post("/api/v1/incidents")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void createIncident_returns400WhenTitleBlank() throws Exception {
        CreateIncidentRequest request = new CreateIncidentRequest("", "Something is down", IncidentSeverity.HIGH,
            IncidentPriority.P1, null);

        mockMvc.perform(post("/api/v1/incidents")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getIncident_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(incidentService.getIncident(id)).thenReturn(sampleResponse(id, IncidentStatus.OPEN));

        mockMvc.perform(get("/api/v1/incidents/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getIncident_returns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(incidentService.getIncident(id)).thenThrow(new IncidentNotFoundException(id));

        mockMvc.perform(get("/api/v1/incidents/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("INCIDENT_NOT_FOUND"));
    }

    @Test
    void searchIncidents_returns200WithPageResponse() throws Exception {
        IncidentSummaryResponse summary = new IncidentSummaryResponse(UUID.randomUUID(), "INC-2026-ABCD1234",
            "Prod outage", IncidentStatus.OPEN, IncidentSeverity.HIGH, IncidentPriority.P1, null, Instant.now());
        when(incidentService.searchIncidents(0, 20, null, null, null))
            .thenReturn(new PageResponse<>(List.of(summary), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/incidents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].title").value("Prod outage"));
    }

    @Test
    void updateIncident_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateIncidentRequest request = new UpdateIncidentRequest("New title", "New description");
        when(incidentService.updateIncident(eq(id), eq(request))).thenReturn(sampleResponse(id,
            IncidentStatus.OPEN));

        mockMvc.perform(put("/api/v1/incidents/{id}", id)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    void assignIncident_returns200AndUsesPrincipalAsActor() throws Exception {
        UUID id = UUID.randomUUID();
        UUID assignee = UUID.randomUUID();
        AssignIncidentRequest request = new AssignIncidentRequest(assignee);
        when(incidentService.assignIncident(eq(id), eq(request), eq(actorId))).thenReturn(sampleResponse(id,
            IncidentStatus.ACKNOWLEDGED));

        mockMvc.perform(put("/api/v1/incidents/{id}/assign", id)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
    }

    @Test
    void assignIncident_returns409OnInvalidTransition() throws Exception {
        UUID id = UUID.randomUUID();
        AssignIncidentRequest request = new AssignIncidentRequest(UUID.randomUUID());
        when(incidentService.assignIncident(eq(id), eq(request), eq(actorId)))
            .thenThrow(new InvalidStatusTransitionException("CLOSED", "ACKNOWLEDGED"));

        mockMvc.perform(put("/api/v1/incidents/{id}/assign", id)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    void resolveIncident_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        ResolveIncidentRequest request = new ResolveIncidentRequest("Root caused and fixed");
        when(incidentService.resolveIncident(eq(id), eq(request), eq(actorId))).thenReturn(sampleResponse(id,
            IncidentStatus.RESOLVED));

        mockMvc.perform(put("/api/v1/incidents/{id}/resolve", id)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void closeIncident_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(incidentService.closeIncident(id, actorId)).thenReturn(sampleResponse(id, IncidentStatus.CLOSED));

        mockMvc.perform(put("/api/v1/incidents/{id}/close", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void startIncident_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(incidentService.startIncident(id, actorId)).thenReturn(sampleResponse(id,
            IncidentStatus.IN_PROGRESS));

        mockMvc.perform(put("/api/v1/incidents/{id}/start", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void addComment_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        AddCommentRequest request = new AddCommentRequest("Investigating now");
        CommentResponse response = new CommentResponse(UUID.randomUUID(), "Investigating now", actorId,
            Instant.now());
        when(incidentService.addComment(eq(id), eq(request), eq(actorId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/incidents/{id}/comments", id)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.message").value("Investigating now"));
    }

    @Test
    void uploadAttachment_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf",
            "content".getBytes());
        AttachmentResponse response = new AttachmentResponse(UUID.randomUUID(), "report.pdf", "application/pdf",
            7L, actorId, Instant.now());
        when(incidentService.uploadAttachment(eq(id), any(), eq(actorId))).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/incidents/{id}/attachments", id).file(file))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.fileName").value("report.pdf"));
    }

    @Test
    void listAttachments_returns200WithArray() throws Exception {
        UUID id = UUID.randomUUID();
        when(incidentService.listAttachments(id)).thenReturn(List.of(
            new AttachmentResponse(UUID.randomUUID(), "a.txt", "text/plain", 1L, actorId, Instant.now())));

        mockMvc.perform(get("/api/v1/incidents/{id}/attachments", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].fileName").value("a.txt"));
    }

    @Test
    void downloadAttachment_defaultsDispositionToAttachment() throws Exception {
        UUID id = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        org.springframework.core.io.Resource resource = new org.springframework.core.io.ByteArrayResource(
            "hello".getBytes());
        when(incidentService.downloadAttachment(id, attachmentId)).thenReturn(
            new com.sirp.incident.incident.dto.response.AttachmentFile(resource, "report.txt", "text/plain"));

        mockMvc.perform(get("/api/v1/incidents/{id}/attachments/{attachmentId}/download", id, attachmentId))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"report.txt\""));
    }

    @Test
    void downloadAttachment_whitelistsDispositionQueryParam() throws Exception {
        UUID id = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        org.springframework.core.io.Resource resource = new org.springframework.core.io.ByteArrayResource(
            "hello".getBytes());
        when(incidentService.downloadAttachment(id, attachmentId)).thenReturn(
            new com.sirp.incident.incident.dto.response.AttachmentFile(resource, "report.txt", "text/plain"));

        mockMvc.perform(get("/api/v1/incidents/{id}/attachments/{attachmentId}/download", id, attachmentId)
                .param("disposition", "javascript:alert(1)"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"report.txt\""));

        mockMvc.perform(get("/api/v1/incidents/{id}/attachments/{attachmentId}/download", id, attachmentId)
                .param("disposition", "inline"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "inline; filename=\"report.txt\""));
    }
}
