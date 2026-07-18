package com.sirp.notification.notification.handler.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sirp.common.enums.IncidentPriority;
import com.sirp.common.enums.IncidentSeverity;
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
import com.sirp.notification.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationEventHandlerImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationDispatcher notificationDispatcher;
    @Mock
    private ResilientUserClient userClient;
    @Mock
    private EmailTemplateRenderer renderer;

    @InjectMocks
    private NotificationEventHandlerImpl handler;

    private UUID incidentId;
    private UUID recipientId;
    private UserNotificationResponse enabledUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "publicBaseUrl", "http://localhost:8080");
        incidentId = UUID.randomUUID();
        recipientId = UUID.randomUUID();
        enabledUser = new UserNotificationResponse(recipientId, "jdoe", "Jane", "Doe", "jdoe@sirp.local",
            "+15551234567", true);
        lenient().when(renderer.renderIncidentCreated(any())).thenReturn("<html>created</html>");
        lenient().when(renderer.renderIncidentAssigned(any())).thenReturn("<html>assigned</html>");
        lenient().when(renderer.renderIncidentResolved(any())).thenReturn("<html>resolved</html>");
        lenient().when(renderer.renderIncidentClosed(any())).thenReturn("<html>closed</html>");
        lenient().when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    class HandleIncidentCreated {

        @Test
        void createsOneNotificationPerChannelForAnEnabledRecipient() {
            IncidentCreatedEvent event = new IncidentCreatedEvent(UUID.randomUUID(), incidentId, "INC-2026-1",
                "Prod outage", "desc", IncidentPriority.P1, IncidentSeverity.HIGH, recipientId, Instant.now());
            when(userClient.findNotificationUser(recipientId)).thenReturn(enabledUser);

            handler.handleIncidentCreated(event);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository, times(NotificationChannel.values().length * 2)).save(captor.capture());
            assertThat(captor.getAllValues().stream().map(Notification::getChannel).distinct())
                .containsExactlyInAnyOrder(NotificationChannel.values());
            assertThat(captor.getAllValues()).allSatisfy(n -> {
                assertThat(n.getType()).isEqualTo(NotificationType.INCIDENT_CREATED);
                assertThat(n.getIncidentId()).isEqualTo(incidentId);
                assertThat(n.getRecipientId()).isEqualTo(recipientId);
                assertThat(n.getRecipientEmail()).isEqualTo("jdoe@sirp.local");
            });
        }

        @Test
        void buildsEmailModelWithOpenStatusAndIncidentUrl() {
            UUID eventId = UUID.randomUUID();
            Instant occurredAt = Instant.now();
            IncidentCreatedEvent event = new IncidentCreatedEvent(eventId, incidentId, "INC-2026-1", "Prod outage",
                "desc", IncidentPriority.P1, IncidentSeverity.HIGH, recipientId, occurredAt);
            when(userClient.findNotificationUser(recipientId)).thenReturn(enabledUser);

            handler.handleIncidentCreated(event);

            ArgumentCaptor<IncidentEmailModel> modelCaptor = ArgumentCaptor.forClass(IncidentEmailModel.class);
            verify(renderer).renderIncidentCreated(modelCaptor.capture());
            IncidentEmailModel model = modelCaptor.getValue();
            assertThat(model.status()).isEqualTo("OPEN");
            assertThat(model.incidentNumber()).isEqualTo("INC-2026-1");
            assertThat(model.incidentUrl()).isEqualTo("http://localhost:8080/api/v1/incidents/" + incidentId);
        }

        @Test
        void skipsNotificationEntirelyWhenRecipientDisabled() {
            IncidentCreatedEvent event = new IncidentCreatedEvent(UUID.randomUUID(), incidentId, "INC-2026-1",
                "Prod outage", "desc", IncidentPriority.P1, IncidentSeverity.HIGH, recipientId, Instant.now());
            UserNotificationResponse disabledUser = new UserNotificationResponse(recipientId, "jdoe", "Jane",
                "Doe", "jdoe@sirp.local", "+15551234567", false);
            when(userClient.findNotificationUser(recipientId)).thenReturn(disabledUser);

            handler.handleIncidentCreated(event);

            verify(notificationRepository, never()).save(any());
            verify(notificationDispatcher, never()).dispatch(any());
        }

        @Test
        void dedupesPerEventIdAndChannelWithoutTouchingDispatcher() {
            IncidentCreatedEvent event = new IncidentCreatedEvent(UUID.randomUUID(), incidentId, "INC-2026-1",
                "Prod outage", "desc", IncidentPriority.P1, IncidentSeverity.HIGH, recipientId, Instant.now());
            when(userClient.findNotificationUser(recipientId)).thenReturn(enabledUser);
            when(notificationRepository.existsByEventIdAndChannel(event.eventId(), NotificationChannel.EMAIL))
                .thenReturn(true);

            handler.handleIncidentCreated(event);

            verify(notificationRepository, times((NotificationChannel.values().length - 1) * 2)).save(any());
            verify(notificationDispatcher, times(NotificationChannel.values().length - 1)).dispatch(any());
        }
    }

    @Nested
    class HandleIncidentAssignedResolvedClosed {

        @Test
        void assignedUsesAssignedToAsRecipientAndAcknowledgedStatus() {
            UUID assignedBy = UUID.randomUUID();
            IncidentAssignedEvent event = new IncidentAssignedEvent(UUID.randomUUID(), incidentId, "INC-2026-1",
                "t", IncidentPriority.P1, IncidentSeverity.HIGH, recipientId, assignedBy, Instant.now());
            when(userClient.findNotificationUser(recipientId)).thenReturn(enabledUser);

            handler.handleIncidentAssigned(event);

            verify(userClient).findNotificationUser(recipientId);
            ArgumentCaptor<IncidentEmailModel> modelCaptor = ArgumentCaptor.forClass(IncidentEmailModel.class);
            verify(renderer).renderIncidentAssigned(modelCaptor.capture());
            assertThat(modelCaptor.getValue().status()).isEqualTo("ACKNOWLEDGED");
            assertThat(modelCaptor.getValue().createdBy()).isEqualTo(assignedBy.toString());
        }

        @Test
        void resolvedUsesResolvedByAsRecipientAndResolvedStatus() {
            IncidentResolvedEvent event = new IncidentResolvedEvent(UUID.randomUUID(), incidentId, "INC-2026-1",
                "t", IncidentPriority.P1, IncidentSeverity.HIGH, recipientId, Instant.now());
            when(userClient.findNotificationUser(recipientId)).thenReturn(enabledUser);

            handler.handleIncidentResolved(event);

            ArgumentCaptor<IncidentEmailModel> modelCaptor = ArgumentCaptor.forClass(IncidentEmailModel.class);
            verify(renderer).renderIncidentResolved(modelCaptor.capture());
            assertThat(modelCaptor.getValue().status()).isEqualTo("RESOLVED");
        }

        @Test
        void closedUsesClosedByAsRecipientAndClosedStatus() {
            IncidentClosedEvent event = new IncidentClosedEvent(UUID.randomUUID(), incidentId, "INC-2026-1", "t",
                IncidentPriority.P1, IncidentSeverity.HIGH, recipientId, Instant.now());
            when(userClient.findNotificationUser(recipientId)).thenReturn(enabledUser);

            handler.handleIncidentClosed(event);

            ArgumentCaptor<IncidentEmailModel> modelCaptor = ArgumentCaptor.forClass(IncidentEmailModel.class);
            verify(renderer).renderIncidentClosed(modelCaptor.capture());
            assertThat(modelCaptor.getValue().status()).isEqualTo("CLOSED");
        }
    }

    @Nested
    class DispatchAndRecordOutcome {

        @Test
        void marksSentWhenDispatchSucceeds() {
            IncidentCreatedEvent event = new IncidentCreatedEvent(UUID.randomUUID(), incidentId, "INC-2026-1",
                "t", "d", IncidentPriority.P1, IncidentSeverity.HIGH, recipientId, Instant.now());
            when(userClient.findNotificationUser(recipientId)).thenReturn(enabledUser);

            handler.handleIncidentCreated(event);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository, times(NotificationChannel.values().length * 2)).save(captor.capture());
            assertThat(captor.getAllValues()).allSatisfy(n -> {
                assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT);
                assertThat(n.getSentAt()).isNotNull();
            });
        }

        @Test
        void marksFailedWithReasonAndDoesNotPropagateWhenDispatchThrows() {
            IncidentCreatedEvent event = new IncidentCreatedEvent(UUID.randomUUID(), incidentId, "INC-2026-1",
                "t", "d", IncidentPriority.P1, IncidentSeverity.HIGH, recipientId, Instant.now());
            when(userClient.findNotificationUser(recipientId)).thenReturn(enabledUser);
            doThrow(new IllegalStateException("SMTP down")).when(notificationDispatcher).dispatch(any());

            handler.handleIncidentCreated(event);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository, times(NotificationChannel.values().length * 2)).save(captor.capture());
            assertThat(captor.getAllValues()).allSatisfy(n -> {
                assertThat(n.getStatus()).isEqualTo(NotificationStatus.FAILED);
                assertThat(n.getFailureReason()).isEqualTo("SMTP down");
                assertThat(n.getSentAt()).isNull();
            });
        }
    }
}
