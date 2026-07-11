package com.sirp.notification.notification.repository;

import com.sirp.notification.notification.entity.Notification;
import com.sirp.notification.notification.enums.NotificationChannel;
import com.sirp.notification.notification.enums.NotificationStatus;
import com.sirp.notification.notification.enums.NotificationType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NotificationRepository
    extends JpaRepository<Notification, UUID>,
    JpaSpecificationExecutor<Notification> {

    Optional<Notification> findByEventId(UUID eventId);

    List<Notification> findByIncidentId(UUID incidentId);

    List<Notification> findByRecipientId(UUID recipientId);

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findByChannel(NotificationChannel channel);

    List<Notification> findByType(NotificationType type);

    boolean existsByEventIdAndChannel(
        UUID eventId,
        NotificationChannel channel);

}