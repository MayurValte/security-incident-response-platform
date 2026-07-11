package com.sirp.notification.notification.sender;

import com.sirp.notification.notification.entity.Notification;
import com.sirp.notification.notification.enums.NotificationChannel;

public interface NotificationSender {

    NotificationChannel getChannel();

    void send(Notification notification);

}