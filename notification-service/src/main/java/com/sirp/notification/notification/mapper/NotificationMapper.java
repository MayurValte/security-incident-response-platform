package com.sirp.notification.notification.mapper;

import com.sirp.notification.notification.dto.response.NotificationResponse;
import com.sirp.notification.notification.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);

}