package com.sirp.incident.incident.service;

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
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface IncidentService {

    IncidentResponse createIncident(CreateIncidentRequest request, UUID actorId);

    IncidentResponse getIncident(UUID id);

    PageResponse<IncidentSummaryResponse> searchIncidents(Integer page, Integer size, String status,
        String severity, String priority);

    IncidentResponse updateIncident(UUID id, UpdateIncidentRequest request);

    /**
     * actorId is the JWT-verified caller when reached via the public
     * controller, or null when triggered internally by workflow-service
     * (Feign calls carry no JWT) - callers should fall back to a sensible
     * default rather than fabricate an actor.
     */
    IncidentResponse assignIncident(UUID id, AssignIncidentRequest request, UUID actorId);

    IncidentResponse resolveIncident(UUID id, ResolveIncidentRequest request, UUID actorId);

    IncidentResponse closeIncident(UUID id, UUID actorId);

    IncidentResponse startIncident(UUID id, UUID actorId);

    CommentResponse addComment(UUID id, AddCommentRequest request, UUID actorId);

    AttachmentResponse uploadAttachment(UUID id, MultipartFile file, UUID actorId);

    List<AttachmentResponse> listAttachments(UUID id);

    AttachmentFile downloadAttachment(UUID id, UUID attachmentId);
}