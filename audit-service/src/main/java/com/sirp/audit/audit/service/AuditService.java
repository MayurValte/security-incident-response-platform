package com.sirp.audit.audit.service;

import com.sirp.audit.audit.dto.response.AuditResponse;
import com.sirp.audit.audit.dto.response.AuditSummaryResponse;
import com.sirp.audit.audit.entity.AggregateType;
import com.sirp.audit.audit.entity.AuditEventType;
import com.sirp.common.dto.PageResponse;
import java.time.Instant;
import java.util.UUID;

public interface AuditService {

  AuditResponse getById(UUID id);

  PageResponse<AuditSummaryResponse> getByAggregateId(
      UUID aggregateId,
      Integer page,
      Integer size
                                    );

  PageResponse<AuditSummaryResponse> getByPerformedBy(
      UUID performedBy,
      Integer page,
      Integer size
                                    );

  PageResponse<AuditSummaryResponse> getByEventType(
      AuditEventType eventType,
      Integer page,
      Integer size
                                  );

  PageResponse<AuditSummaryResponse> search(
      UUID aggregateId,
      AggregateType aggregateType,
      AuditEventType eventType,
      UUID performedBy,
      Instant from,
      Instant to,
      Integer page,
      Integer size
                          );

}