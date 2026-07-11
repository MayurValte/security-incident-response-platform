package com.sirp.notification.notification.dto.request;

import com.sirp.notification.notification.enums.NotificationChannel;
import com.sirp.notification.notification.enums.NotificationStatus;
import com.sirp.notification.notification.enums.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationSearchRequest(

    UUID incidentId,

    UUID recipientId,

    NotificationStatus status,

    NotificationChannel channel,

    NotificationType type,

    Instant createdAfter,

    Instant createdBefore

) {

}