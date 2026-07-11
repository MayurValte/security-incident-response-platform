package com.sirp.incident.kafka.producer;

import com.sirp.common.events.IncidentAssignedEvent;
import com.sirp.common.events.IncidentClosedEvent;
import com.sirp.common.events.IncidentCreatedEvent;
import com.sirp.common.events.IncidentResolvedEvent;
import com.sirp.common.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class IncidentEventProducerImpl implements IncidentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishCreated(IncidentCreatedEvent event) {
        kafkaTemplate.send(KafkaTopics.INCIDENT_CREATED, event.incidentId().toString(), event);
        log.info("Published IncidentCreatedEvent [{}]", event.eventId());
    }

    @Override
    public void publishAssigned(IncidentAssignedEvent event) {
        kafkaTemplate.send(KafkaTopics.INCIDENT_ASSIGNED, event.incidentId().toString(), event);
        log.info("Published IncidentAssignedEvent [{}]", event.eventId());
    }

    @Override
    public void publishResolved(IncidentResolvedEvent event) {
        kafkaTemplate.send(KafkaTopics.INCIDENT_RESOLVED, event.incidentId().toString(), event);
        log.info("Published IncidentResolvedEvent [{}]", event.eventId());
    }

    @Override
    public void publishClosed(IncidentClosedEvent event) {
        kafkaTemplate.send(KafkaTopics.INCIDENT_CLOSED, event.incidentId().toString(), event);
        log.info("Published IncidentClosedEvent [{}]", event.eventId());
    }
}