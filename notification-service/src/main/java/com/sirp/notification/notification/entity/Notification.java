package com.sirp.notification.notification.entity;

import com.sirp.notification.notification.enums.NotificationChannel;
import com.sirp.notification.notification.enums.NotificationStatus;
import com.sirp.notification.notification.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notification_event_id", columnList = "event_id"),
        @Index(name = "idx_notification_incident_id", columnList = "incident_id"),
        @Index(name = "idx_notification_recipient_id", columnList = "recipient_id"),
        @Index(name = "idx_notification_status", columnList = "status"),
        @Index(name = "idx_notification_channel", columnList = "channel")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    private UUID id;

    @Column(
        name = "event_id",
        nullable = false,
        updatable = false
    )
    private UUID eventId;

    @Column(
        name = "incident_id",
        nullable = false,
        updatable = false
    )
    private UUID incidentId;

    @Column(
        name = "recipient_id",
        nullable = false
    )
    private UUID recipientId;

    @Column(
        name = "recipient_email",
        length = 255
    )
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(length = 255)
    private String subject;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(length = 1000)
    private String failureReason;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant sentAt;

    @Column(name = "recipient_phone", length = 30)
    private String recipientPhone;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

}