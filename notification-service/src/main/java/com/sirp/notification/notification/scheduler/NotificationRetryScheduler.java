package com.sirp.notification.notification.scheduler;

import com.sirp.notification.notification.dispatcher.NotificationDispatcher;
import com.sirp.notification.notification.entity.Notification;
import com.sirp.notification.notification.enums.NotificationStatus;
import com.sirp.notification.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Without this, a FAILED notification (see NotificationEventHandlerImpl's
 * dispatchAndRecordOutcome - dispatch failures are deliberately not
 * rethrown, so Kafka-level redelivery never retries the send) just sits
 * in that state forever with no remediation path. This sweep picks those
 * rows back up periodically. Capped at MAX_RETRY_ATTEMPTS per
 * notification - findByStatusAndRetryCountLessThan naturally stops
 * returning a row once it's exhausted its attempts, so a permanently
 * broken recipient (e.g. invalid email) doesn't get retried forever;
 * there's no separate "gave up" status, since retryCount reaching the cap
 * already answers that without inventing a new state.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRetryScheduler {

    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final NotificationRepository notificationRepository;
    private final NotificationDispatcher notificationDispatcher;

    @Scheduled(fixedRateString = "${notification.retry.check-interval-ms:600000}")
    @Transactional
    public void retryFailedNotifications() {
        List<Notification> failed =
            notificationRepository.findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, MAX_RETRY_ATTEMPTS);

        for (Notification notification : failed) {
            retry(notification);
        }
    }

    private void retry(Notification notification) {
        notification.setRetryCount(notification.getRetryCount() + 1);
        try {
            notificationDispatcher.dispatch(notification);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
            notification.setFailureReason(null);
            log.info("Retry {} succeeded for notification {}", notification.getRetryCount(), notification.getId());
        } catch (Exception ex) {
            notification.setFailureReason(ex.getMessage());
            log.warn("Retry {} failed for notification {}: {}", notification.getRetryCount(), notification.getId(),
                ex.toString());
        } finally {
            notificationRepository.save(notification);
        }
    }
}
