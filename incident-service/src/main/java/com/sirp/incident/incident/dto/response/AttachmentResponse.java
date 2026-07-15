package com.sirp.incident.incident.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AttachmentResponse(

    UUID id,

    String fileName,

    String contentType,

    Long fileSize,

    UUID uploadedBy,

    Instant uploadedAt

) {
}
