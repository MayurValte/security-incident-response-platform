package com.sirp.audit.audit.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sirp.audit.audit.dto.response.AuditPageResponse;
import com.sirp.audit.audit.dto.response.AuditResponse;
import com.sirp.audit.audit.dto.response.AuditSummaryResponse;
import com.sirp.audit.audit.entity.AggregateType;
import com.sirp.audit.audit.entity.AuditEvent;
import com.sirp.audit.audit.entity.AuditEventType;
import com.sirp.audit.audit.exception.AuditNotFoundException;
import com.sirp.audit.audit.mapper.AuditMapper;
import com.sirp.audit.audit.repository.AuditEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private AuditEventRepository repository;

    @Mock
    private AuditMapper mapper;

    @InjectMocks
    private AuditServiceImpl auditService;

    private UUID id;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
    }

    @Nested
    class GetById {

        @Test
        void returnsMappedResponseWhenFound() {
            AuditEvent event = AuditEvent.builder().id(id).build();
            AuditResponse response = new AuditResponse(id, UUID.randomUUID(), UUID.randomUUID(),
                AggregateType.INCIDENT, AuditEventType.INCIDENT_CREATED, "incident-service", UUID.randomUUID(),
                Instant.now(), "{}");
            when(repository.findById(id)).thenReturn(Optional.of(event));
            when(mapper.toResponse(event)).thenReturn(response);

            assertThat(auditService.getById(id)).isEqualTo(response);
        }

        @Test
        void throwsAuditNotFoundWhenMissing() {
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> auditService.getById(id)).isInstanceOf(AuditNotFoundException.class);
        }
    }

    @Test
    void getByAggregateIdBuildsPageableSortedByOccurredAtDescending() {
        UUID aggregateId = UUID.randomUUID();
        AuditEvent event = AuditEvent.builder().id(id).aggregateId(aggregateId).build();
        Page<AuditEvent> page = new PageImpl<>(List.of(event));
        AuditSummaryResponse summary = new AuditSummaryResponse(id, UUID.randomUUID(), aggregateId,
            AggregateType.INCIDENT, AuditEventType.INCIDENT_CREATED, "incident-service", null, Instant.now());
        AuditPageResponse pageResponse = new AuditPageResponse(List.of(summary), 0, 20, 1L, 1, true, true);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(repository.findByAggregateId(eq(aggregateId), pageableCaptor.capture())).thenReturn(page);
        when(mapper.toPageResponse(page)).thenReturn(pageResponse);

        AuditPageResponse result = auditService.getByAggregateId(aggregateId, 0, 20);

        assertThat(result).isEqualTo(pageResponse);
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("occurredAt").isDescending()).isTrue();
    }

    @Test
    void getByPerformedByDelegatesToRepository() {
        UUID performedBy = UUID.randomUUID();
        Page<AuditEvent> page = new PageImpl<>(List.of());
        AuditPageResponse pageResponse = new AuditPageResponse(List.of(), 0, 20, 0L, 0, true, true);
        when(repository.findByPerformedBy(eq(performedBy), any(Pageable.class))).thenReturn(page);
        when(mapper.toPageResponse(page)).thenReturn(pageResponse);

        assertThat(auditService.getByPerformedBy(performedBy, 0, 20)).isEqualTo(pageResponse);
    }

    @Test
    void getByEventTypeDelegatesToRepository() {
        Page<AuditEvent> page = new PageImpl<>(List.of());
        AuditPageResponse pageResponse = new AuditPageResponse(List.of(), 0, 20, 0L, 0, true, true);
        when(repository.findByEventType(eq(AuditEventType.LOGIN_FAILED), any(Pageable.class))).thenReturn(page);
        when(mapper.toPageResponse(page)).thenReturn(pageResponse);

        assertThat(auditService.getByEventType(AuditEventType.LOGIN_FAILED, 0, 20)).isEqualTo(pageResponse);
    }

    @Test
    void searchCombinesAllFiltersIntoASingleSpecificationQuery() {
        Page<AuditEvent> page = new PageImpl<>(List.of());
        AuditPageResponse pageResponse = new AuditPageResponse(List.of(), 0, 20, 0L, 0, true, true);
        when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<AuditEvent>>any(),
            any(Pageable.class))).thenReturn(page);
        when(mapper.toPageResponse(page)).thenReturn(pageResponse);

        AuditPageResponse result = auditService.search(UUID.randomUUID(), AggregateType.INCIDENT,
            AuditEventType.INCIDENT_CREATED, UUID.randomUUID(), Instant.now().minusSeconds(3600), Instant.now(),
            0, 20);

        assertThat(result).isEqualTo(pageResponse);
        verify(repository).findAll(org.mockito.ArgumentMatchers.<Specification<AuditEvent>>any(),
            any(Pageable.class));
    }

    @Test
    void searchWorksWithAllFiltersNull() {
        Page<AuditEvent> page = new PageImpl<>(List.of());
        AuditPageResponse pageResponse = new AuditPageResponse(List.of(), 0, 20, 0L, 0, true, true);
        when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<AuditEvent>>any(),
            any(Pageable.class))).thenReturn(page);
        when(mapper.toPageResponse(page)).thenReturn(pageResponse);

        assertThat(auditService.search(null, null, null, null, null, null, 0, 20)).isEqualTo(pageResponse);
    }
}
