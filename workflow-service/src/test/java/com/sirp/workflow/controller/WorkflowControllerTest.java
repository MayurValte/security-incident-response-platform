package com.sirp.workflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirp.common.enums.IncidentSeverity;
import com.sirp.common.enums.WorkflowStatus;
import com.sirp.workflow.dto.request.AssignWorkflowRequest;
import com.sirp.workflow.dto.request.CloseWorkflowRequest;
import com.sirp.workflow.dto.request.CreateWorkflowRequest;
import com.sirp.workflow.dto.request.EscalateWorkflowRequest;
import com.sirp.workflow.dto.request.ResolveWorkflowRequest;
import com.sirp.workflow.dto.response.WorkflowResponse;
import com.sirp.workflow.exception.GlobalExceptionHandler;
import com.sirp.workflow.exception.InvalidWorkflowStateException;
import com.sirp.workflow.exception.WorkflowNotFoundException;
import com.sirp.workflow.service.AssignmentService;
import com.sirp.workflow.service.EscalationService;
import com.sirp.workflow.service.ResolutionService;
import com.sirp.workflow.service.WorkflowService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class WorkflowControllerTest {

    @Mock
    private WorkflowService workflowService;
    @Mock
    private AssignmentService assignmentService;
    @Mock
    private EscalationService escalationService;
    @Mock
    private ResolutionService resolutionService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private UUID actorId;

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();
        WorkflowController controller = new WorkflowController(workflowService, assignmentService,
            escalationService, resolutionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(new LocalValidatorFactoryBean())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver(),
                new PageableHandlerMethodArgumentResolver())
            .build();

        actorId = UUID.randomUUID();
        JwtUser principal = new JwtUser(actorId, "jdoe@sirp.local", "ENGINEER", null);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(principal, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private WorkflowResponse sampleResponse(UUID id, UUID incidentId, WorkflowStatus status) {
        return new WorkflowResponse(id, incidentId, null, null, status, IncidentSeverity.HIGH, 0,
            Instant.now(), null, null, null, null, Instant.now(), Instant.now());
    }

    @Test
    void create_returns201WithActorFromPrincipal() throws Exception {
        UUID incidentId = UUID.randomUUID();
        CreateWorkflowRequest request = new CreateWorkflowRequest(incidentId, null, null, IncidentSeverity.HIGH,
            Instant.now().plusSeconds(3600), null, null);
        UUID id = UUID.randomUUID();
        when(workflowService.createWorkflow(eq(request), eq(actorId))).thenReturn(
            sampleResponse(id, incidentId, WorkflowStatus.CREATED));

        mockMvc.perform(post("/api/v1/workflows")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.workflowStatus").value("CREATED"));
    }

    @Test
    void create_returns409WhenWorkflowAlreadyExists() throws Exception {
        UUID incidentId = UUID.randomUUID();
        CreateWorkflowRequest request = new CreateWorkflowRequest(incidentId, null, null, IncidentSeverity.HIGH,
            Instant.now().plusSeconds(3600), null, null);
        when(workflowService.createWorkflow(eq(request), eq(actorId)))
            .thenThrow(new com.sirp.workflow.exception.WorkflowAlreadyExistsException("dup"));

        mockMvc.perform(post("/api/v1/workflows")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    void getById_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(workflowService.getWorkflowById(id)).thenReturn(sampleResponse(id, UUID.randomUUID(),
            WorkflowStatus.CREATED));

        mockMvc.perform(get("/api/v1/workflows/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getById_returns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(workflowService.getWorkflowById(id)).thenThrow(new WorkflowNotFoundException("not found"));

        mockMvc.perform(get("/api/v1/workflows/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("WORKFLOW_NOT_FOUND"));
    }

    @Test
    void getByIncidentId_returns200() throws Exception {
        UUID incidentId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(workflowService.getWorkflowByIncidentId(incidentId)).thenReturn(sampleResponse(id, incidentId,
            WorkflowStatus.CREATED));

        mockMvc.perform(get("/api/v1/workflows/incident/{incidentId}", incidentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.incidentId").value(incidentId.toString()));
    }

    @Test
    void getAll_returns200WithPageBody() throws Exception {
        WorkflowResponse response = sampleResponse(UUID.randomUUID(), UUID.randomUUID(), WorkflowStatus.CREATED);
        when(workflowService.getAllWorkflows(any())).thenReturn(new PageImpl<>(List.of(response),
            PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/workflows"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].workflowStatus").value("CREATED"));
    }

    @Test
    void getByAssignedUser_returns200WithPlainArray() throws Exception {
        UUID userId = UUID.randomUUID();
        WorkflowResponse response = sampleResponse(UUID.randomUUID(), UUID.randomUUID(), WorkflowStatus.ASSIGNED);
        when(workflowService.getWorkflowsByAssignedUser(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/workflows/user/{userId}", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].workflowStatus").value("ASSIGNED"));
    }

    @Test
    void getByAssignedTeam_returns200WithPlainArray() throws Exception {
        UUID teamId = UUID.randomUUID();
        WorkflowResponse response = sampleResponse(UUID.randomUUID(), UUID.randomUUID(), WorkflowStatus.ASSIGNED);
        when(workflowService.getWorkflowsByAssignedTeam(teamId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/workflows/team/{teamId}", teamId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].workflowStatus").value("ASSIGNED"));
    }

    @Test
    void assign_returns200AndUsesPrincipalAsActor() throws Exception {
        UUID id = UUID.randomUUID();
        UUID assignee = UUID.randomUUID();
        AssignWorkflowRequest request = new AssignWorkflowRequest(assignee, null);
        when(assignmentService.assignWorkflow(eq(id), eq(request), eq(actorId))).thenReturn(
            sampleResponse(id, UUID.randomUUID(), WorkflowStatus.ASSIGNED));

        mockMvc.perform(put("/api/v1/workflows/{id}/assign", id)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workflowStatus").value("ASSIGNED"));
    }

    @Test
    void reassign_returns400WhenNeverAssignedBefore() throws Exception {
        UUID id = UUID.randomUUID();
        AssignWorkflowRequest request = new AssignWorkflowRequest(UUID.randomUUID(), null);
        when(assignmentService.reassignWorkflow(eq(id), eq(request), eq(actorId)))
            .thenThrow(new InvalidWorkflowStateException("use assign instead"));

        mockMvc.perform(put("/api/v1/workflows/{id}/reassign", id)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void escalate_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        EscalateWorkflowRequest request = new EscalateWorkflowRequest(Instant.now().plusSeconds(1800), "remarks");
        when(escalationService.escalateWorkflow(eq(id), eq(request), eq(actorId))).thenReturn(
            sampleResponse(id, UUID.randomUUID(), WorkflowStatus.ESCALATED));

        mockMvc.perform(put("/api/v1/workflows/{id}/escalate", id)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workflowStatus").value("ESCALATED"));
    }

    @Test
    void resolve_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        ResolveWorkflowRequest request = new ResolveWorkflowRequest("fixed");
        when(resolutionService.resolveWorkflow(eq(id), eq(request), eq(actorId))).thenReturn(
            sampleResponse(id, UUID.randomUUID(), WorkflowStatus.RESOLVED));

        mockMvc.perform(put("/api/v1/workflows/{id}/resolve", id)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workflowStatus").value("RESOLVED"));
    }

    @Test
    void close_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        CloseWorkflowRequest request = new CloseWorkflowRequest("done");
        when(resolutionService.closeWorkflow(eq(id), eq(request), eq(actorId))).thenReturn(
            sampleResponse(id, UUID.randomUUID(), WorkflowStatus.CLOSED));

        mockMvc.perform(put("/api/v1/workflows/{id}/close", id)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workflowStatus").value("CLOSED"));
    }

    @Test
    void delete_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/workflows/{id}", id))
            .andExpect(status().isNoContent());

        verify(workflowService).deleteWorkflow(id);
    }
}
