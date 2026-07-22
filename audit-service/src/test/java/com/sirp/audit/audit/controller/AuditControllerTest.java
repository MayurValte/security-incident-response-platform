package com.sirp.audit.audit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sirp.audit.audit.dto.response.AuditResponse;
import com.sirp.audit.audit.dto.response.AuditSummaryResponse;
import com.sirp.audit.audit.entity.AggregateType;
import com.sirp.audit.audit.entity.AuditEventType;
import com.sirp.audit.audit.exception.AuditNotFoundException;
import com.sirp.audit.audit.exception.GlobalExceptionHandler;
import com.sirp.audit.audit.service.AuditService;
import com.sirp.common.dto.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    @Mock
    private AuditService auditService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuditController controller = new AuditController(auditService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void getById_returns200WithFullPayload() throws Exception {
        UUID id = UUID.randomUUID();
        AuditResponse response = new AuditResponse(id, UUID.randomUUID(), UUID.randomUUID(),
            AggregateType.INCIDENT, AuditEventType.INCIDENT_CREATED, "incident-service", UUID.randomUUID(),
            Instant.now(), "{\"title\":\"x\"}");
        when(auditService.getById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/audits/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.eventType").value("INCIDENT_CREATED"))
            .andExpect(jsonPath("$.payload").value("{\"title\":\"x\"}"));
    }

    @Test
    void getById_returns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(auditService.getById(id)).thenThrow(new AuditNotFoundException(id));

        mockMvc.perform(get("/api/v1/audits/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Audit event not found : " + id));
    }

    @Test
    void getByAggregate_returns200WithPageResponseOmittingPayload() throws Exception {
        UUID aggregateId = UUID.randomUUID();
        AuditSummaryResponse summary = new AuditSummaryResponse(UUID.randomUUID(), UUID.randomUUID(),
            aggregateId, AggregateType.INCIDENT, AuditEventType.INCIDENT_CREATED, "incident-service", null,
            Instant.now());
        when(auditService.getByAggregateId(eq(aggregateId), eq(0), eq(20)))
            .thenReturn(new PageResponse<>(List.of(summary), 0, 20, 1L, 1, true, true));

        mockMvc.perform(get("/api/v1/audits/aggregate/{aggregateId}", aggregateId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].aggregateId").value(aggregateId.toString()))
            .andExpect(jsonPath("$.content[0].payload").doesNotExist());
    }

    @Test
    void getByAggregate_honorsExplicitPageAndSize() throws Exception {
        UUID aggregateId = UUID.randomUUID();
        when(auditService.getByAggregateId(eq(aggregateId), eq(2), eq(5)))
            .thenReturn(new PageResponse<>(List.of(), 2, 5, 0L, 0, false, true));

        mockMvc.perform(get("/api/v1/audits/aggregate/{aggregateId}", aggregateId)
                .param("page", "2")
                .param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(2))
            .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void getByPerformedBy_returns200() throws Exception {
        UUID performedBy = UUID.randomUUID();
        when(auditService.getByPerformedBy(eq(performedBy), eq(0), eq(20)))
            .thenReturn(new PageResponse<>(List.of(), 0, 20, 0L, 0, true, true));

        mockMvc.perform(get("/api/v1/audits/user/{performedBy}", performedBy))
            .andExpect(status().isOk());
    }

    @Test
    void getByEventType_parsesPathVariableAsEnum() throws Exception {
        when(auditService.getByEventType(eq(AuditEventType.LOGIN_FAILED), eq(0), eq(20)))
            .thenReturn(new PageResponse<>(List.of(), 0, 20, 0L, 0, true, true));

        mockMvc.perform(get("/api/v1/audits/event-type/{eventType}", "LOGIN_FAILED"))
            .andExpect(status().isOk());
    }

    @Test
    void search_passesAllQueryParamsThrough() throws Exception {
        UUID aggregateId = UUID.randomUUID();
        UUID performedBy = UUID.randomUUID();
        when(auditService.search(eq(aggregateId), eq(AggregateType.INCIDENT), eq(AuditEventType.INCIDENT_CREATED),
            eq(performedBy), any(Instant.class), any(Instant.class), eq(0), eq(20)))
            .thenReturn(new PageResponse<>(List.of(), 0, 20, 0L, 0, true, true));

        mockMvc.perform(get("/api/v1/audits")
                .param("aggregateId", aggregateId.toString())
                .param("aggregateType", "INCIDENT")
                .param("eventType", "INCIDENT_CREATED")
                .param("performedBy", performedBy.toString())
                .param("from", "2026-01-01T00:00:00Z")
                .param("to", "2026-12-31T00:00:00Z"))
            .andExpect(status().isOk());
    }

    @Test
    void search_defaultsAllFiltersToNullWhenOmitted() throws Exception {
        when(auditService.search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(20)))
            .thenReturn(new PageResponse<>(List.of(), 0, 20, 0L, 0, true, true));

        mockMvc.perform(get("/api/v1/audits"))
            .andExpect(status().isOk());
    }
}
