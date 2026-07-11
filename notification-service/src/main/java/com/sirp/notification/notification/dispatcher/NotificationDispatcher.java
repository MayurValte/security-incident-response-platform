package com.sirp.notification.notification.dispatcher;

import com.sirp.notification.notification.entity.Notification;

public interface NotificationDispatcher {

    void dispatch(Notification notification);

}