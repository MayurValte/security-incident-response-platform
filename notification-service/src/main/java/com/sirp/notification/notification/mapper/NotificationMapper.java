package com.sirp.notification.notification.mapper;

import com.sirp.notification.notification.dto.response.NotificationResponse;
import com.sirp.notification.notification.entity.Notification;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);

    List<NotificationResponse> toResponseList(List<Notification> notifications);

}