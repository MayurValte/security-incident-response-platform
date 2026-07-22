package com.sirp.audit.audit.mapper;

import com.sirp.audit.audit.dto.response.AuditResponse;
import com.sirp.audit.audit.dto.response.AuditSummaryResponse;
import com.sirp.audit.audit.entity.AuditEvent;
import com.sirp.common.dto.PageResponse;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface AuditMapper {

  AuditResponse toResponse(AuditEvent auditEvent);

  AuditSummaryResponse toSummary(AuditEvent auditEvent);

  default PageResponse<AuditSummaryResponse> toPageResponse(Page<AuditEvent> page) {

    return new PageResponse<>(

        page.getContent()
            .stream()
            .map(this::toSummary)
            .toList(),

        page.getNumber(),

        page.getSize(),

        page.getTotalElements(),

        page.getTotalPages(),

        page.isFirst(),

        page.isLast()

    );

  }

}