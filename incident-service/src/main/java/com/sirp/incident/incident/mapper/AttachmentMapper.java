package com.sirp.incident.incident.mapper;

import com.sirp.incident.incident.dto.response.AttachmentResponse;
import com.sirp.incident.incident.entity.IncidentAttachment;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttachmentMapper {

    AttachmentResponse toResponse(IncidentAttachment attachment);

    List<AttachmentResponse> toResponseList(List<IncidentAttachment> attachments);
}
