package com.sirp.incident.incident.helper;

import com.sirp.common.enums.IncidentPriority;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class SlaCalculator {

    public Instant calculate(IncidentPriority priority) {
        return switch (priority) {
            case P1 -> Instant.now().plus(1, ChronoUnit.HOURS);
            case P2 -> Instant.now().plus(4, ChronoUnit.HOURS);
            case P3 -> Instant.now().plus(24, ChronoUnit.HOURS);
            case P4 -> Instant.now().plus(72, ChronoUnit.HOURS);
        };
    }
}