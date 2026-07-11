package com.sirp.notification.notification.sender;

import com.sirp.notification.notification.entity.Notification;
import com.sirp.notification.notification.enums.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InAppNotificationSender implements NotificationSender {

    @Override
    public NotificationChannel getChannel() {

        return NotificationChannel.IN_APP;

    }

    @Override
    public void send(Notification notification) {

        log.info(
            "In-App notification available for recipient {}",
            notification.getRecipientId());

    }

}