package com.sirp.notification.notification.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sirp.notification.notification.dispatcher.NotificationDispatcher;
import com.sirp.notification.notification.entity.Notification;
import com.sirp.notification.notification.enums.NotificationChannel;
import com.sirp.notification.notification.enums.NotificationStatus;
import com.sirp.notification.notification.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationRetrySchedulerTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationDispatcher notificationDispatcher;

    @InjectMocks
    private NotificationRetryScheduler scheduler;

    @Test
    void marksSentAndClearsFailureReasonWhenRetrySucceeds() {
        Notification notification = Notification.builder().id(UUID.randomUUID()).channel(NotificationChannel.EMAIL)
            .status(NotificationStatus.FAILED).failureReason("SMTP down").retryCount(1).build();
        when(notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3))
            .thenReturn(List.of(notification));

        scheduler.retryFailedNotifications();

        assertThat(notification.getRetryCount()).isEqualTo(2);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getFailureReason()).isNull();
        assertThat(notification.getSentAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }

    @Test
    void incrementsRetryCountAndKeepsFailureReasonWhenRetryFailsAgain() {
        Notification notification = Notification.builder().id(UUID.randomUUID()).channel(NotificationChannel.EMAIL)
            .status(NotificationStatus.FAILED).failureReason("SMTP down").retryCount(1).build();
        when(notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3))
            .thenReturn(List.of(notification));
        doThrow(new IllegalStateException("still down")).when(notificationDispatcher).dispatch(notification);

        scheduler.retryFailedNotifications();

        assertThat(notification.getRetryCount()).isEqualTo(2);
        assertThat(notification.getFailureReason()).isEqualTo("still down");
        verify(notificationRepository).save(notification);
    }

    @Test
    void retriesEveryNotificationReturnedByTheRepositoryQuery() {
        Notification first = Notification.builder().id(UUID.randomUUID()).channel(NotificationChannel.EMAIL)
            .status(NotificationStatus.FAILED).retryCount(0).build();
        Notification second = Notification.builder().id(UUID.randomUUID()).channel(NotificationChannel.SMS)
            .status(NotificationStatus.FAILED).retryCount(2).build();
        when(notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3))
            .thenReturn(List.of(first, second));

        scheduler.retryFailedNotifications();

        verify(notificationDispatcher).dispatch(first);
        verify(notificationDispatcher).dispatch(second);
        assertThat(first.getRetryCount()).isEqualTo(1);
        assertThat(second.getRetryCount()).isEqualTo(3);
    }

    @Test
    void doesNothingWhenNoFailedNotificationsBelowRetryCap() {
        when(notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3))
            .thenReturn(List.of());

        scheduler.retryFailedNotifications();

        verify(notificationDispatcher, never()).dispatch(any());
        verify(notificationRepository, never()).save(any());
    }
}
