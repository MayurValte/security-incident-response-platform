package com.sirp.workflow.controller;

import com.sirp.workflow.dto.request.AssignWorkflowRequest;
import com.sirp.workflow.dto.request.CloseWorkflowRequest;
import com.sirp.workflow.dto.request.CreateWorkflowRequest;
import com.sirp.workflow.dto.request.EscalateWorkflowRequest;
import com.sirp.workflow.dto.request.ResolveWorkflowRequest;
import com.sirp.workflow.dto.response.WorkflowResponse;
import com.sirp.workflow.service.AssignmentService;
import com.sirp.workflow.service.EscalationService;
import com.sirp.workflow.service.ResolutionService;
import com.sirp.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
@Tag(name = "Workflow API", description = "Workflow and escalation management APIs")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final AssignmentService assignmentService;
    private final EscalationService escalationService;
    private final ResolutionService resolutionService;

    @PostMapping
    @Operation(summary = "Create a workflow for an incident")
    public ResponseEntity<WorkflowResponse> create(@Valid @RequestBody CreateWorkflowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.createWorkflow(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get workflow by id")
    public ResponseEntity<WorkflowResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(workflowService.getWorkflowById(id));
    }

    @GetMapping("/incident/{incidentId}")
    @Operation(summary = "Get workflow by incident id")
    public ResponseEntity<WorkflowResponse> getByIncidentId(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(workflowService.getWorkflowByIncidentId(incidentId));
    }

    @GetMapping
    @Operation(summary = "List all workflows")
    public ResponseEntity<Page<WorkflowResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(workflowService.getAllWorkflows(pageable));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List workflows assigned to a user")
    public ResponseEntity<List<WorkflowResponse>> getByAssignedUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(workflowService.getWorkflowsByAssignedUser(userId));
    }

    @GetMapping("/team/{teamId}")
    @Operation(summary = "List workflows assigned to a team")
    public ResponseEntity<List<WorkflowResponse>> getByAssignedTeam(@PathVariable UUID teamId) {
        return ResponseEntity.ok(workflowService.getWorkflowsByAssignedTeam(teamId));
    }

    @PutMapping("/{id}/assign")
    @Operation(summary = "Assign a workflow")
    public ResponseEntity<WorkflowResponse> assign(@PathVariable UUID id,
        @Valid @RequestBody AssignWorkflowRequest request) {
        return ResponseEntity.ok(assignmentService.assignWorkflow(id, request));
    }

    @PutMapping("/{id}/reassign")
    @Operation(summary = "Reassign an already-assigned workflow")
    public ResponseEntity<WorkflowResponse> reassign(@PathVariable UUID id,
        @Valid @RequestBody AssignWorkflowRequest request) {
        return ResponseEntity.ok(assignmentService.reassignWorkflow(id, request));
    }

    @PutMapping("/{id}/escalate")
    @Operation(summary = "Escalate a workflow")
    public ResponseEntity<WorkflowResponse> escalate(@PathVariable UUID id,
        @Valid @RequestBody EscalateWorkflowRequest request) {
        return ResponseEntity.ok(escalationService.escalateWorkflow(id, request));
    }

    @PutMapping("/{id}/resolve")
    @Operation(summary = "Resolve a workflow")
    public ResponseEntity<WorkflowResponse> resolve(@PathVariable UUID id,
        @Valid @RequestBody ResolveWorkflowRequest request) {
        return ResponseEntity.ok(resolutionService.resolveWorkflow(id, request));
    }

    @PutMapping("/{id}/close")
    @Operation(summary = "Close a resolved workflow")
    public ResponseEntity<WorkflowResponse> close(@PathVariable UUID id,
        @Valid @RequestBody CloseWorkflowRequest request) {
        return ResponseEntity.ok(resolutionService.closeWorkflow(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a workflow")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        workflowService.deleteWorkflow(id);
        return ResponseEntity.noContent().build();
    }
}
