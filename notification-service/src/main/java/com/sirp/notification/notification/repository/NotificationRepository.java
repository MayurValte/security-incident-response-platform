package com.sirp.notification.notification.repository;

import com.sirp.notification.notification.entity.Notification;
import com.sirp.notification.notification.enums.NotificationChannel;
import com.sirp.notification.notification.enums.NotificationStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NotificationRepository
    extends JpaRepository<Notification, UUID>,
    JpaSpecificationExecutor<Notification> {

    List<Notification> findByIncidentId(UUID incidentId);

    List<Notification> findByRecipientId(UUID recipientId);

    boolean existsByEventIdAndChannel(
        UUID eventId,
        NotificationChannel channel);

    List<Notification> findByStatusAndRetryCountLessThan(NotificationStatus status, int maxRetryCount);

}