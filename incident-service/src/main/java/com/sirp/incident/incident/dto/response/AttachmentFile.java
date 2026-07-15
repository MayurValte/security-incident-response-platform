package com.sirp.incident.incident.dto.response;

import org.springframework.core.io.Resource;

public record AttachmentFile(

        Resource resource,

        String fileName,

        String contentType

) {
}
