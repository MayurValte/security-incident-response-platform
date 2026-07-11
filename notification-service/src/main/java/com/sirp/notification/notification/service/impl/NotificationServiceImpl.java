package com.sirp.notification.notification.service.impl;

import com.sirp.notification.exception.NotificationNotFoundException;
import com.sirp.notification.notification.dto.request.NotificationSearchRequest;
import com.sirp.notification.notification.dto.response.NotificationPageResponse;
import com.sirp.notification.notification.dto.response.NotificationResponse;
import com.sirp.notification.notification.entity.Notification;
import com.sirp.notification.notification.mapper.NotificationMapper;
import com.sirp.notification.notification.repository.NotificationRepository;
import com.sirp.notification.notification.service.NotificationService;
import com.sirp.notification.notification.specification.NotificationSpecification;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    private final NotificationMapper notificationMapper;

    @Override
    public NotificationResponse getNotification(UUID id) {

        Notification notification =
            notificationRepository.findById(id)
                                  .orElseThrow(() -> new NotificationNotFoundException(id));

        return notificationMapper.toResponse(notification);

    }

    @Override
    public NotificationPageResponse searchNotifications(
        Integer page,
        Integer size,
        NotificationSearchRequest request) {

        Pageable pageable =
            PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Notification> specification = Specification.allOf(

            NotificationSpecification.incidentId(
                request.incidentId()),

            NotificationSpecification.recipientId(
                request.recipientId()),

            NotificationSpecification.status(
                request.status()),

            NotificationSpecification.channel(
                request.channel()),

            NotificationSpecification.type(
                request.type()),

            NotificationSpecification.createdAfter(
                request.createdAfter()),

            NotificationSpecification.createdBefore(
                request.createdBefore())

                                                                       );

        Page<Notification> notifications =
            notificationRepository.findAll(
                specification,
                pageable);

        List<NotificationResponse> responses =
            notifications.stream()
                         .map(notificationMapper::toResponse)
                         .toList();

        return new NotificationPageResponse(

            responses,

            notifications.getNumber(),

            notifications.getSize(),

            notifications.getTotalElements(),

            notifications.getTotalPages(),

            notifications.isFirst(),

            notifications.isLast()

        );

    }

    @Override
    public List<NotificationResponse> getNotificationsByIncident(
        UUID incidentId) {

        return notificationRepository
            .findByIncidentId(incidentId)
            .stream()
            .map(notificationMapper::toResponse)
            .toList();

    }

    @Override
    public List<NotificationResponse> getNotificationsByRecipient(
        UUID recipientId) {

        return notificationRepository
            .findByRecipientId(recipientId)
            .stream()
            .map(notificationMapper::toResponse)
            .toList();

    }

}