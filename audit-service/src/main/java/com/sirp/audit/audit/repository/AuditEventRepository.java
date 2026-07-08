package com.sirp.audit.audit.repository;

import com.sirp.audit.audit.entity.AggregateType;
import com.sirp.audit.audit.entity.AuditEvent;
import com.sirp.audit.audit.entity.AuditEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID>, JpaSpecificationExecutor<AuditEvent> {
    boolean existsByEventId(
            UUID eventId
    );

    Page<AuditEvent> findByAggregateId(

            UUID aggregateId,

            Pageable pageable

    );

    Page<AuditEvent> findByPerformedBy(

            UUID performedBy,

            Pageable pageable

    );

    Page<AuditEvent> findByEventType(

            AuditEventType eventType,

            Pageable pageable

    );

    Page<AuditEvent> findByAggregateType(

            AggregateType aggregateType,

            Pageable pageable

    );
}