package com.sirp.notification.notification.handler.impl;

import com.sirp.common.events.IncidentAssignedEvent;
import com.sirp.common.events.IncidentClosedEvent;
import com.sirp.common.events.IncidentCreatedEvent;
import com.sirp.common.events.IncidentResolvedEvent;
import com.sirp.notification.email.model.IncidentEmailModel;
import com.sirp.notification.email.template.EmailTemplateRenderer;
import com.sirp.notification.feign.ResilientUserClient;
import com.sirp.notification.feign.dto.UserNotificationResponse;
import com.sirp.notification.notification.dispatcher.NotificationDispatcher;
import com.sirp.notification.notification.entity.Notification;
import com.sirp.notification.notification.enums.NotificationChannel;
import com.sirp.notification.notification.enums.NotificationStatus;
import com.sirp.notification.notification.enums.NotificationType;
import com.sirp.notification.notification.handler.NotificationEventHandler;
import com.sirp.notification.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationEventHandlerImpl implements NotificationEventHandler {

    private final NotificationRepository notificationRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final ResilientUserClient userClient;
    private final EmailTemplateRenderer renderer;

    @Override
    public void handleIncidentCreated(IncidentCreatedEvent event) {
        IncidentEmailModel model = new IncidentEmailModel(event.incidentNumber(),
                                                          event.title(),
                                                          event.description(),
                                                          event.priority().name(),
                                                          event.severity().name(),
                                                          "OPEN",
                                                          event.createdBy().toString(),
                                                          event.occurredAt(),
                                                          "http://localhost:8080/api/v1/incidents/" +
                                                              event.incidentId());

        String html = renderer.renderIncidentCreated(model);
        createNotifications(event.eventId(), event.incidentId(), event.createdBy(), NotificationType.INCIDENT_CREATED,
                            "Incident Created", html);
    }

    @Override
    public void handleIncidentAssigned(IncidentAssignedEvent event) {
        IncidentEmailModel model = new IncidentEmailModel(event.incidentNumber(),
                                                          event.title(),
                                                          "",
                                                          event.priority().name(),
                                                          event.severity().name(),
                                                          "ACKNOWLEDGED",
                                                          event.assignedBy().toString(),
                                                          event.occurredAt(),
                                                          "http://localhost:8080/api/v1/incidents/" +
                                                              event.incidentId());

        String html = renderer.renderIncidentAssigned(model);
        createNotifications(event.eventId(), event.incidentId(), event.assignedTo(), NotificationType.INCIDENT_ASSIGNED,
                            "Incident Assigned", html);
    }

    @Override
    public void handleIncidentResolved(IncidentResolvedEvent event) {
        IncidentEmailModel model = new IncidentEmailModel(event.incidentNumber(),
                                                          event.title(),
                                                          "",
                                                          event.priority().name(),
                                                          event.severity().name(),
                                                          "RESOLVED",
                                                          event.resolvedBy().toString(),
                                                          event.occurredAt(),
                                                          "http://localhost:8080/api/v1/incidents/" +
                                                              event.incidentId());

        String html = renderer.renderIncidentResolved(model);
        createNotifications(event.eventId(), event.incidentId(), event.resolvedBy(), NotificationType.INCIDENT_RESOLVED,
                            "Incident Resolved", html);
    }

    @Override
    public void handleIncidentClosed(IncidentClosedEvent event) {
        IncidentEmailModel model = new IncidentEmailModel(event.incidentNumber(),
                                                          event.title(),
                                                          "",
                                                          event.priority().name(),
                                                          event.severity().name(),
                                                          "CLOSED",
                                                          event.closedBy().toString(),
                                                          event.occurredAt(),
                                                          "http://localhost:8080/api/v1/incidents/" +
                                                              event.incidentId());

        String html = renderer.renderIncidentClosed(model);
        createNotifications(event.eventId(), event.incidentId(), event.closedBy(), NotificationType.INCIDENT_CLOSED,
                            "Incident Closed", html);
    }

    private void createNotifications(UUID eventId,
        UUID incidentId,
        UUID recipientId,
        NotificationType type,
        String subject,
        String message) {
        UserNotificationResponse user = userClient.findNotificationUser(recipientId);
        if (user == null || !user.enabled()) {
            log.warn("Notification skipped because recipient {} is unavailable or disabled.", recipientId);
            return;
        }
        for (NotificationChannel channel : NotificationChannel.values()) {
            saveNotification(eventId, incidentId, recipientId, user, channel, type, subject, message);
        }
    }

    private void saveNotification(UUID eventId,
        UUID incidentId,
        UUID recipientId,
        UserNotificationResponse user,
        NotificationChannel channel,
        NotificationType type,
        String subject,
        String message) {
        if (notificationRepository.existsByEventIdAndChannel(eventId, channel)) {
            log.debug("Notification already exists for event {} and channel {}", eventId, channel);
            return;
        }
        Notification notification = Notification.builder()
                                                .id(UUID.randomUUID())
                                                .eventId(eventId)
                                                .incidentId(incidentId)
                                                .recipientId(recipientId)
                                                .recipientEmail(user.email())
                                                .recipientPhone(user.phoneNumber())
                                                .channel(channel)
                                                .type(type)
                                                .status(NotificationStatus.PENDING)
                                                .subject(subject)
                                                .message(message)
                                                .createdAt(Instant.now())
                                                .build();
        Notification saved = notificationRepository.save(notification);
        dispatchAndRecordOutcome(saved, recipientId, channel);
    }

    /**
     * Dispatch failures (e.g. a transient SMTP error) are deliberately
     * NOT rethrown here. The caller's existsByEventIdAndChannel guard
     * means a Kafka-level redelivery retry (see KafkaErrorHandlingConfig)
     * would find this row already persisted and skip straight past it
     * without ever calling dispatch() again - so letting a dispatch
     * failure propagate up to the @KafkaListener would trigger a retry
     * that can never actually retry the thing that failed. Recording
     * FAILED here instead keeps the listener-level retry/DLT policy
     * meaningful for what it can actually fix (the DB/lookup steps
     * before this point), while a failed send needs its own remediation
     * (a re-send job or admin action against FAILED rows) rather than
     * Kafka redelivery.
     */
    private void dispatchAndRecordOutcome(Notification saved, UUID recipientId, NotificationChannel channel) {
        try {
            notificationDispatcher.dispatch(saved);
            saved.setStatus(NotificationStatus.SENT);
            saved.setSentAt(Instant.now());
            log.info("Notification {} sent to {} via {}", saved.getId(), recipientId, channel);
        } catch (Exception ex) {
            saved.setStatus(NotificationStatus.FAILED);
            saved.setFailureReason(ex.getMessage());
            log.error("Notification {} failed to send to {} via {}: {}", saved.getId(), recipientId, channel,
                ex.toString());
        } finally {
            notificationRepository.save(saved);
        }
    }
}