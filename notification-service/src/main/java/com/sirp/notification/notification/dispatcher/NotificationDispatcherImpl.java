package com.sirp.notification.notification.dispatcher;

import com.sirp.notification.notification.entity.Notification;
import com.sirp.notification.notification.enums.NotificationChannel;
import com.sirp.notification.notification.sender.NotificationSender;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationDispatcherImpl implements NotificationDispatcher {

    private final Map<NotificationChannel, NotificationSender> senders;

    public NotificationDispatcherImpl(List<NotificationSender> senderList) {

        this.senders = new EnumMap<>(NotificationChannel.class);

        senderList.forEach(sender ->
                               this.senders.put(sender.getChannel(), sender));

    }

    @Override
    public void dispatch(Notification notification) {

        NotificationSender sender = senders.get(notification.getChannel());

        if (sender == null) {

            throw new IllegalStateException(
                "No NotificationSender found for channel : "
                    + notification.getChannel());

        }

        log.info(
            "Dispatching notification {} using {}",
            notification.getId(),
            notification.getChannel());

        sender.send(notification);

    }

}