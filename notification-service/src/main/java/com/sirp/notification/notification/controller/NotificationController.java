package com.sirp.notification.notification.controller;

import com.sirp.notification.notification.dto.request.NotificationSearchRequest;
import com.sirp.notification.notification.dto.response.NotificationPageResponse;
import com.sirp.notification.notification.dto.response.NotificationResponse;
import com.sirp.notification.notification.enums.NotificationChannel;
import com.sirp.notification.notification.enums.NotificationStatus;
import com.sirp.notification.notification.enums.NotificationType;
import com.sirp.notification.notification.service.NotificationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getNotification(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationService.getNotification(id));
    }

    @GetMapping
    public ResponseEntity<NotificationPageResponse> searchNotifications(@RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "10") Integer size,
        @RequestParam(required = false) UUID incidentId,
        @RequestParam(required = false) UUID recipientId,
        @RequestParam(required = false) NotificationStatus status,
        @RequestParam(required = false) NotificationChannel channel,
        @RequestParam(required = false) NotificationType type,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant createdAfter,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant createdBefore) {
        NotificationSearchRequest request = new NotificationSearchRequest(incidentId, recipientId, status, channel,
                                                                          type, createdAfter, createdBefore);
        return ResponseEntity.ok(notificationService.searchNotifications(page, size, request));
    }

    @GetMapping("/incident/{incidentId}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByIncident(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(notificationService.getNotificationsByIncident(incidentId));
    }

    @GetMapping("/recipient/{recipientId}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByRecipient(@PathVariable UUID recipientId) {
        return ResponseEntity.ok(notificationService.getNotificationsByRecipient(recipientId));
    }
}