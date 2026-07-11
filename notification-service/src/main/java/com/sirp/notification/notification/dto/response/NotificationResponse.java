package com.sirp.notification.notification.dto.response;

import com.sirp.notification.notification.enums.NotificationChannel;
import com.sirp.notification.notification.enums.NotificationStatus;
import com.sirp.notification.notification.enums.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(

    UUID id,

    UUID eventId,

    UUID incidentId,

    UUID recipientId,

    String recipientEmail,

    NotificationChannel channel,

    NotificationType type,

    NotificationStatus status,

    String subject,

    String message,

    String failureReason,

    Instant createdAt,

    Instant sentAt

) {

}