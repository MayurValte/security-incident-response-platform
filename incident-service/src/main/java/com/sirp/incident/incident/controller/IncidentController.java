package com.sirp.incident.incident.controller;

import com.sirp.incident.incident.dto.request.AddCommentRequest;
import com.sirp.incident.incident.dto.request.AssignIncidentRequest;
import com.sirp.incident.incident.dto.request.CreateIncidentRequest;
import com.sirp.incident.incident.dto.request.ResolveIncidentRequest;
import com.sirp.incident.incident.dto.request.UpdateIncidentRequest;
import com.sirp.common.dto.PageResponse;
import com.sirp.incident.incident.dto.response.AttachmentFile;
import com.sirp.incident.incident.dto.response.AttachmentResponse;
import com.sirp.incident.incident.dto.response.CommentResponse;
import com.sirp.incident.incident.dto.response.IncidentResponse;
import com.sirp.incident.incident.dto.response.IncidentSummaryResponse;
import com.sirp.incident.incident.service.IncidentService;
import com.sirp.security.model.JwtUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/incidents")
@Tag(name = "Incident API", description = "Incident Management APIs")
public class IncidentController {

    private final IncidentService incidentService;

    @Operation(summary = "Create Incident")
    @PostMapping
    public ResponseEntity<IncidentResponse> createIncident(@RequestBody @Valid CreateIncidentRequest request,
        @AuthenticationPrincipal JwtUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(incidentService.createIncident(request, principal.userId()));
    }

    @Operation(summary = "Get Incident")
    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getIncident(@PathVariable UUID id) {
        return ResponseEntity.ok(incidentService.getIncident(id));
    }

    @Operation(summary = "Search Incidents")
    @GetMapping
    public ResponseEntity<PageResponse<IncidentSummaryResponse>> searchIncidents(
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String severity,
        @RequestParam(required = false) String priority) {
        return ResponseEntity.ok(incidentService.searchIncidents(page, size, status, severity, priority));
    }

    @Operation(summary = "Update Incident")
    @PutMapping("/{id}")
    public ResponseEntity<IncidentResponse> updateIncident(@PathVariable UUID id,
        @RequestBody @Valid UpdateIncidentRequest request) {
        return ResponseEntity.ok(incidentService.updateIncident(id, request));
    }

    @Operation(summary = "Assign Incident")
    @PutMapping("/{id}/assign")
    public ResponseEntity<IncidentResponse> assignIncident(@PathVariable UUID id,
        @RequestBody @Valid AssignIncidentRequest request, @AuthenticationPrincipal JwtUser principal) {
        return ResponseEntity.ok(incidentService.assignIncident(id, request, principal.userId()));
    }

    @Operation(summary = "Resolve Incident")
    @PutMapping("/{id}/resolve")
    public ResponseEntity<IncidentResponse> resolveIncident(@PathVariable UUID id,
        @RequestBody @Valid ResolveIncidentRequest request, @AuthenticationPrincipal JwtUser principal) {
        return ResponseEntity.ok(incidentService.resolveIncident(id, request, principal.userId()));
    }

    @Operation(summary = "Close Incident")
    @PutMapping("/{id}/close")
    public ResponseEntity<IncidentResponse> closeIncident(@PathVariable UUID id,
        @AuthenticationPrincipal JwtUser principal) {
        return ResponseEntity.ok(incidentService.closeIncident(id, principal.userId()));
    }

    @PutMapping("/{id}/start")
    public ResponseEntity<IncidentResponse> startIncident(@PathVariable UUID id,
        @AuthenticationPrincipal JwtUser principal) {
        return ResponseEntity.ok(incidentService.startIncident(id, principal.userId()));
    }

    @Operation(summary = "Add Comment")
    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable UUID id,
        @RequestBody @Valid AddCommentRequest request, @AuthenticationPrincipal JwtUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(incidentService.addComment(id, request, principal.userId()));
    }

    @Operation(summary = "Upload Attachment")
    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> uploadAttachment(@PathVariable UUID id,
        @RequestParam("file") MultipartFile file, @AuthenticationPrincipal JwtUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(incidentService.uploadAttachment(id, file, principal.userId()));
    }

    @Operation(summary = "List Attachments")
    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<AttachmentResponse>> listAttachments(@PathVariable UUID id) {
        return ResponseEntity.ok(incidentService.listAttachments(id));
    }

    @Operation(summary = "Download or View Attachment")
    @GetMapping("/{id}/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable UUID id, @PathVariable UUID attachmentId,
        @RequestParam(name = "disposition", defaultValue = "attachment") String disposition) {
        AttachmentFile file = incidentService.downloadAttachment(id, attachmentId);
        MediaType contentType = file.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM
            : MediaType.parseMediaType(file.contentType());
        String headerSafeName = file.fileName().replace("\"", "");
        // Whitelisted, not passed through raw - disposition comes from a query
        // param and must never be interpolated into the header unchecked.
        String safeDisposition = "inline".equalsIgnoreCase(disposition) ? "inline" : "attachment";
        return ResponseEntity.ok()
                             .contentType(contentType)
                             .header(HttpHeaders.CONTENT_DISPOSITION,
                                 safeDisposition + "; filename=\"" + headerSafeName + "\"")
                             .body(file.resource());
    }
}