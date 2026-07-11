package com.sirp.incident.incident.specification;

import com.sirp.common.enums.IncidentPriority;
import com.sirp.common.enums.IncidentSeverity;
import com.sirp.incident.incident.entity.Incident;
import com.sirp.incident.incident.enums.IncidentStatus;
import org.springframework.data.jpa.domain.Specification;

public final class IncidentSpecification {

    private IncidentSpecification() {
    }

    public static Specification<Incident> hasStatus(IncidentStatus status) {
        if (status == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Incident> hasSeverity(IncidentSeverity severity) {
        if (severity == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("severity"), severity);
    }

    public static Specification<Incident> hasPriority(IncidentPriority priority) {
        if (priority == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }
}