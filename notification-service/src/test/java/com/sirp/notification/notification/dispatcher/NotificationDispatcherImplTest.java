package com.sirp.notification.notification.dispatcher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sirp.notification.notification.entity.Notification;
import com.sirp.notification.notification.enums.NotificationChannel;
import com.sirp.notification.notification.sender.NotificationSender;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationDispatcherImplTest {

    @Test
    void dispatchesToTheSenderMatchingTheNotificationChannel() {
        NotificationSender emailSender = mockSender(NotificationChannel.EMAIL);
        NotificationSender smsSender = mockSender(NotificationChannel.SMS);
        NotificationDispatcherImpl dispatcher = new NotificationDispatcherImpl(List.of(emailSender, smsSender));
        Notification notification = Notification.builder().id(UUID.randomUUID())
            .channel(NotificationChannel.SMS).build();

        dispatcher.dispatch(notification);

        verify(smsSender).send(notification);
        verify(emailSender, org.mockito.Mockito.never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void throwsIllegalStateWhenNoSenderRegisteredForChannel() {
        NotificationSender emailSender = mockSender(NotificationChannel.EMAIL);
        NotificationDispatcherImpl dispatcher = new NotificationDispatcherImpl(List.of(emailSender));
        Notification notification = Notification.builder().id(UUID.randomUUID())
            .channel(NotificationChannel.SMS).build();

        assertThatThrownBy(() -> dispatcher.dispatch(notification))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SMS");
    }

    private NotificationSender mockSender(NotificationChannel channel) {
        NotificationSender sender = org.mockito.Mockito.mock(NotificationSender.class);
        when(sender.getChannel()).thenReturn(channel);
        return sender;
    }
}
