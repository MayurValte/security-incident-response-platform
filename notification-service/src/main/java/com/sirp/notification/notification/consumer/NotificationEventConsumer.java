package com.sirp.notification.notification.consumer;

import com.sirp.common.events.IncidentAssignedEvent;
import com.sirp.common.events.IncidentClosedEvent;
import com.sirp.common.events.IncidentCreatedEvent;
import com.sirp.common.events.IncidentResolvedEvent;
import com.sirp.common.kafka.KafkaTopics;
import com.sirp.notification.notification.handler.NotificationEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationEventHandler handler;

    @KafkaListener(
        topics = KafkaTopics.INCIDENT_CREATED,
        groupId = "notification-service")
    public void consumeCreated(IncidentCreatedEvent event) {

        log.info("Received IncidentCreatedEvent {}", event.incidentId());

        handler.handleIncidentCreated(event);

    }

    @KafkaListener(
        topics = KafkaTopics.INCIDENT_ASSIGNED,
        groupId = "notification-service")
    public void consumeAssigned(IncidentAssignedEvent event) {

        log.info("Received IncidentAssignedEvent {}", event.incidentId());

        handler.handleIncidentAssigned(event);

    }

    @KafkaListener(
        topics = KafkaTopics.INCIDENT_RESOLVED,
        groupId = "notification-service")
    public void consumeResolved(IncidentResolvedEvent event) {

        log.info("Received IncidentResolvedEvent {}", event.incidentId());

        handler.handleIncidentResolved(event);

    }

    @KafkaListener(
        topics = KafkaTopics.INCIDENT_CLOSED,
        groupId = "notification-service")
    public void consumeClosed(IncidentClosedEvent event) {

        log.info("Received IncidentClosedEvent {}", event.incidentId());

        handler.handleIncidentClosed(event);

    }

}