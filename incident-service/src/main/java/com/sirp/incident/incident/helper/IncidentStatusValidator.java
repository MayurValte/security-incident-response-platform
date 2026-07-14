package com.sirp.incident.incident.helper;

import com.sirp.incident.exception.InvalidStatusTransitionException;
import com.sirp.incident.incident.enums.IncidentStatus;
import org.springframework.stereotype.Component;

@Component
public class IncidentStatusValidator {

    public void validate(IncidentStatus current, IncidentStatus target) {
        switch (current) {
            case OPEN -> {
                if (target != IncidentStatus.ACKNOWLEDGED) {
                    throw new InvalidStatusTransitionException(current.name(), target.name());
                }
            }
            case ACKNOWLEDGED -> {
                if (target != IncidentStatus.IN_PROGRESS) {
                    throw new InvalidStatusTransitionException(current.name(), target.name());
                }
            }
            case IN_PROGRESS -> {
                if (target != IncidentStatus.RESOLVED) {
                    throw new InvalidStatusTransitionException(current.name(), target.name());
                }
            }
            case RESOLVED -> {
                if (target != IncidentStatus.CLOSED) {
                    throw new InvalidStatusTransitionException(current.name(), target.name());
                }
            }
            case CLOSED -> throw new InvalidStatusTransitionException(current.name(), target.name());
        }
    }
}