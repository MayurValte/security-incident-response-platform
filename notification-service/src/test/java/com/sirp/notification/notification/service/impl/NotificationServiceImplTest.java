package com.sirp.notification.notification.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sirp.common.dto.PageResponse;
import com.sirp.notification.exception.NotificationNotFoundException;
import com.sirp.notification.notification.dto.request.NotificationSearchRequest;
import com.sirp.notification.notification.dto.response.NotificationResponse;
import com.sirp.notification.notification.entity.Notification;
import com.sirp.notification.notification.enums.NotificationChannel;
import com.sirp.notification.notification.enums.NotificationStatus;
import com.sirp.notification.notification.enums.NotificationType;
import com.sirp.notification.notification.mapper.NotificationMapper;
import com.sirp.notification.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UUID id;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
    }

    @Nested
    class GetNotification {

        @Test
        void returnsMappedResponseWhenFound() {
            Notification notification = Notification.builder().id(id).build();
            NotificationResponse response = new NotificationResponse(id, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "jdoe@sirp.local", NotificationChannel.EMAIL, NotificationType.INCIDENT_CREATED,
                NotificationStatus.SENT, "subj", "msg", null, Instant.now(), Instant.now());
            when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));
            when(notificationMapper.toResponse(notification)).thenReturn(response);

            assertThat(notificationService.getNotification(id)).isEqualTo(response);
        }

        @Test
        void throwsNotFoundWhenMissing() {
            when(notificationRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.getNotification(id))
                .isInstanceOf(NotificationNotFoundException.class);
        }
    }

    @Test
    void searchNotificationsBuildsSpecificationAndPagesResults() {
        Notification notification = Notification.builder().id(id).build();
        NotificationResponse response = new NotificationResponse(id, UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), "jdoe@sirp.local", NotificationChannel.EMAIL, NotificationType.INCIDENT_CREATED,
            NotificationStatus.SENT, "subj", "msg", null, Instant.now(), Instant.now());
        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(notificationMapper.toResponse(notification)).thenReturn(response);
        NotificationSearchRequest request = new NotificationSearchRequest(null, null, null, null, null, null,
            null);

        PageResponse<NotificationResponse> result = notificationService.searchNotifications(0, 10, request);

        assertThat(result.content()).containsExactly(response);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void getNotificationsByIncidentReturnsPlainList() {
        UUID incidentId = UUID.randomUUID();
        Notification notification = Notification.builder().id(id).incidentId(incidentId).build();
        NotificationResponse response = new NotificationResponse(id, UUID.randomUUID(), incidentId,
            UUID.randomUUID(), "jdoe@sirp.local", NotificationChannel.EMAIL, NotificationType.INCIDENT_CREATED,
            NotificationStatus.SENT, "subj", "msg", null, Instant.now(), Instant.now());
        when(notificationRepository.findByIncidentId(incidentId)).thenReturn(List.of(notification));
        when(notificationMapper.toResponse(notification)).thenReturn(response);

        assertThat(notificationService.getNotificationsByIncident(incidentId)).containsExactly(response);
    }

    @Test
    void getNotificationsByRecipientReturnsPlainList() {
        UUID recipientId = UUID.randomUUID();
        Notification notification = Notification.builder().id(id).recipientId(recipientId).build();
        NotificationResponse response = new NotificationResponse(id, UUID.randomUUID(), UUID.randomUUID(),
            recipientId, "jdoe@sirp.local", NotificationChannel.EMAIL, NotificationType.INCIDENT_CREATED,
            NotificationStatus.SENT, "subj", "msg", null, Instant.now(), Instant.now());
        when(notificationRepository.findByRecipientId(recipientId)).thenReturn(List.of(notification));
        when(notificationMapper.toResponse(notification)).thenReturn(response);

        assertThat(notificationService.getNotificationsByRecipient(recipientId)).containsExactly(response);
    }
}
