package com.sirp.notification.notification.service;

import com.sirp.notification.notification.dto.request.NotificationSearchRequest;
import com.sirp.notification.notification.dto.response.NotificationPageResponse;
import com.sirp.notification.notification.dto.response.NotificationResponse;
import java.util.List;
import java.util.UUID;

public interface NotificationService {

    NotificationResponse getNotification(UUID id);

    NotificationPageResponse searchNotifications(
        Integer page,
        Integer size,
        NotificationSearchRequest request);

    List<NotificationResponse> getNotificationsByIncident(UUID incidentId);

    List<NotificationResponse> getNotificationsByRecipient(UUID recipientId);

}