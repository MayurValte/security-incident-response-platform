package com.sirp.analytics.consumer;

import com.sirp.analytics.entity.AnalyticsStatus;
import com.sirp.analytics.entity.IncidentAnalyticsRecord;
import com.sirp.analytics.repository.IncidentAnalyticsRepository;
import com.sirp.common.events.IncidentAssignedEvent;
import com.sirp.common.events.IncidentClosedEvent;
import com.sirp.common.events.IncidentCreatedEvent;
import com.sirp.common.events.IncidentResolvedEvent;
import com.sirp.common.kafka.KafkaTopics;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the analytics read model incrementally from the same incident
 * lifecycle events audit-service consumes - but keeps derived current
 * state per incident instead of an immutable event log, since the goal
 * here is aggregation (counts, resolution time), not a legal/compliance
 * audit trail. Runs its own consumer group so it doesn't compete with
 * audit-service for partitions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsEventConsumer {

    private static final String GROUP_ID = "analytics-service";

    private final IncidentAnalyticsRepository repository;

    @KafkaListener(topics = KafkaTopics.INCIDENT_CREATED, groupId = GROUP_ID)
    @Transactional
    public void consumeCreated(IncidentCreatedEvent event) {
        if (repository.existsByIncidentId(event.incidentId())) {
            log.debug("Duplicate IncidentCreatedEvent ignored [{}]", event.incidentId());
            return;
        }

        IncidentAnalyticsRecord record = IncidentAnalyticsRecord.builder()
                                                                .incidentId(event.incidentId())
                                                                .incidentNumber(event.incidentNumber())
                                                                .title(event.title())
                                                                .priority(event.priority())
                                                                .severity(event.severity())
                                                                .status(AnalyticsStatus.CREATED)
                                                                .createdAt(event.occurredAt())
                                                                .build();

        repository.save(record);
        log.info("Analytics record created for incident {}", event.incidentId());
    }

    @KafkaListener(topics = KafkaTopics.INCIDENT_ASSIGNED, groupId = GROUP_ID)
    @Transactional
    public void consumeAssigned(IncidentAssignedEvent event) {
        repository.findByIncidentId(event.incidentId()).ifPresentOrElse(record -> {
            record.setStatus(AnalyticsStatus.ASSIGNED);
            record.setAssignedAt(event.occurredAt());
            repository.save(record);
        }, () -> log.warn("No analytics record for assigned incident {} (event ordering?)", event.incidentId()));
    }

    @KafkaListener(topics = KafkaTopics.INCIDENT_RESOLVED, groupId = GROUP_ID)
    @Transactional
    public void consumeResolved(IncidentResolvedEvent event) {
        repository.findByIncidentId(event.incidentId()).ifPresentOrElse(record -> {
            record.setStatus(AnalyticsStatus.RESOLVED);
            record.setResolvedAt(event.occurredAt());
            record.setResolutionMinutes(Duration.between(record.getCreatedAt(), event.occurredAt()).toMinutes());
            repository.save(record);
        }, () -> log.warn("No analytics record for resolved incident {} (event ordering?)", event.incidentId()));
    }

    @KafkaListener(topics = KafkaTopics.INCIDENT_CLOSED, groupId = GROUP_ID)
    @Transactional
    public void consumeClosed(IncidentClosedEvent event) {
        repository.findByIncidentId(event.incidentId()).ifPresentOrElse(record -> {
            record.setStatus(AnalyticsStatus.CLOSED);
            record.setClosedAt(event.occurredAt());
            repository.save(record);
        }, () -> log.warn("No analytics record for closed incident {} (event ordering?)", event.incidentId()));
    }
}
