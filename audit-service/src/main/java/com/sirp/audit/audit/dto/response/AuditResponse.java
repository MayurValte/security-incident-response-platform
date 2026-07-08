package com.sirp.audit.audit.dto.response;

import com.sirp.audit.audit.entity.AggregateType;
import com.sirp.audit.audit.entity.AuditEventType;
import java.time.Instant;
import java.util.UUID;

public record AuditResponse(

    UUID id,

    UUID eventId,

    UUID aggregateId,

    AggregateType aggregateType,

    AuditEventType eventType,

    String serviceName,

    UUID performedBy,

    Instant occurredAt,

    String payload

) {

}