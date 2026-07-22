package com.sirp.audit.audit.controller;

import com.sirp.audit.audit.dto.response.AuditResponse;
import com.sirp.audit.audit.dto.response.AuditSummaryResponse;
import com.sirp.audit.audit.entity.AggregateType;
import com.sirp.audit.audit.entity.AuditEventType;
import com.sirp.audit.audit.service.AuditService;
import com.sirp.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audits")
@RequiredArgsConstructor
@Tag(name = "Audit API", description = "Audit event management APIs")
public class AuditController {

  private final AuditService auditService;

  @GetMapping("/{id}")
  @Operation(summary = "Get audit event by id")
  public AuditResponse getById(@PathVariable UUID id) {
    return auditService.getById(id);
  }

  @GetMapping("/aggregate/{aggregateId}")
  @Operation(summary = "Get audit events by aggregate id")
  public PageResponse<AuditSummaryResponse> getByAggregate(
      @PathVariable UUID aggregateId,
      @RequestParam(defaultValue = "0") Integer page,
      @RequestParam(defaultValue = "20") Integer size) {
    return auditService.getByAggregateId(aggregateId, page, size);
  }

  @GetMapping("/user/{performedBy}")
  @Operation(summary = "Get audit events by user")
  public PageResponse<AuditSummaryResponse> getByPerformedBy(
      @PathVariable UUID performedBy,
      @RequestParam(defaultValue = "0") Integer page,
      @RequestParam(defaultValue = "20") Integer size) {
    return auditService.getByPerformedBy(performedBy, page, size);
  }

  @GetMapping("/event-type/{eventType}")
  @Operation(summary = "Get audit events by event type")
  public PageResponse<AuditSummaryResponse> getByEventType(
      @PathVariable AuditEventType eventType,
      @RequestParam(defaultValue = "0") Integer page,
      @RequestParam(defaultValue = "20") Integer size) {
    return auditService.getByEventType(eventType, page, size);
  }

  @GetMapping
  @Operation(summary = "Search audit events")
  public PageResponse<AuditSummaryResponse> search(
      @RequestParam(required = false) UUID aggregateId,
      @RequestParam(required = false) AggregateType aggregateType,
      @RequestParam(required = false) AuditEventType eventType,
      @RequestParam(required = false) UUID performedBy,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
      @RequestParam(defaultValue = "0") Integer page,
      @RequestParam(defaultValue = "20") Integer size) {
    return auditService.search(aggregateId, aggregateType, eventType, performedBy, from, to, page,
                               size);
  }
}