package com.sirp.audit.audit.service;

import com.sirp.audit.audit.dto.response.AuditPageResponse;
import com.sirp.audit.audit.dto.response.AuditResponse;
import com.sirp.audit.audit.entity.AggregateType;
import com.sirp.audit.audit.entity.AuditEventType;
import java.time.Instant;
import java.util.UUID;

public interface AuditService {

  AuditResponse getById(UUID id);

  AuditPageResponse getByAggregateId(
      UUID aggregateId,
      Integer page,
      Integer size
                                    );

  AuditPageResponse getByPerformedBy(
      UUID performedBy,
      Integer page,
      Integer size
                                    );

  AuditPageResponse getByEventType(
      AuditEventType eventType,
      Integer page,
      Integer size
                                  );

  AuditPageResponse search(
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