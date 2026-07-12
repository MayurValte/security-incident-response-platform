package com.sirp.workflow.feign;

import com.sirp.workflow.feign.dto.AssignIncidentRequest;
import com.sirp.workflow.feign.dto.IncidentResponse;
import com.sirp.workflow.feign.dto.ResolveIncidentRequest;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "incident-service")
public interface IncidentServiceClient {

    @PutMapping("/internal/incidents/{id}/assign")
    IncidentResponse assignIncident(@PathVariable UUID id,
        @RequestBody AssignIncidentRequest request);

    @PutMapping("/internal/incidents/{id}/resolve")
    IncidentResponse resolveIncident(@PathVariable UUID id,
        @RequestBody ResolveIncidentRequest request);

    @PutMapping("/internal/incidents/{id}/close")
    IncidentResponse closeIncident(@PathVariable UUID id);
}