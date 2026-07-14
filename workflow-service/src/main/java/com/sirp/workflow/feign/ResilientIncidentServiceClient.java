package com.sirp.workflow.feign;

import com.sirp.workflow.exception.IncidentServiceUnavailableException;
import com.sirp.workflow.feign.dto.AssignIncidentRequest;
import com.sirp.workflow.feign.dto.IncidentResponse;
import com.sirp.workflow.feign.dto.ResolveIncidentRequest;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResilientIncidentServiceClient {

    private final IncidentServiceClient incidentServiceClient;

    @CircuitBreaker(name = "incident-service", fallbackMethod = "assignIncidentFallback")
    public IncidentResponse assignIncident(UUID id, AssignIncidentRequest request) {
        return incidentServiceClient.assignIncident(id, request);
    }

    @CircuitBreaker(name = "incident-service", fallbackMethod = "startIncidentFallback")
    public IncidentResponse startIncident(UUID id) {
        return incidentServiceClient.startIncident(id);
    }

    @CircuitBreaker(name = "incident-service", fallbackMethod = "resolveIncidentFallback")
    public IncidentResponse resolveIncident(UUID id, ResolveIncidentRequest request) {
        return incidentServiceClient.resolveIncident(id, request);
    }

    @CircuitBreaker(name = "incident-service", fallbackMethod = "closeIncidentFallback")
    public IncidentResponse closeIncident(UUID id) {
        return incidentServiceClient.closeIncident(id);
    }

    private IncidentResponse assignIncidentFallback(UUID id, AssignIncidentRequest request, Throwable t) {
        log.error("incident-service circuit breaker fallback for assignIncident({}): {}", id, t.toString());
        throw new IncidentServiceUnavailableException("Incident Service unavailable", t);
    }

    private IncidentResponse startIncidentFallback(UUID id, Throwable t) {
        if (t instanceof FeignException.Conflict) {
            log.warn("incident-service rejected startIncident({}) as an invalid transition, "
                    + "assuming it's already past this step: {}", id, t.toString());
            return null;
        }
        log.error("incident-service circuit breaker fallback for startIncident({}): {}", id, t.toString());
        throw new IncidentServiceUnavailableException("Incident Service unavailable", t);
    }

    private IncidentResponse resolveIncidentFallback(UUID id, ResolveIncidentRequest request, Throwable t) {
        log.error("incident-service circuit breaker fallback for resolveIncident({}): {}", id, t.toString());
        throw new IncidentServiceUnavailableException("Incident Service unavailable", t);
    }

    private IncidentResponse closeIncidentFallback(UUID id, Throwable t) {
        log.error("incident-service circuit breaker fallback for closeIncident({}): {}", id, t.toString());
        throw new IncidentServiceUnavailableException("Incident Service unavailable", t);
    }
}
