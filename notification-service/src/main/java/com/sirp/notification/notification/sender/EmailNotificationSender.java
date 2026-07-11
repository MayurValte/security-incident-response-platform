package com.sirp.notification.notification.sender;

import com.sirp.notification.email.EmailService;
import com.sirp.notification.notification.entity.Notification;
import com.sirp.notification.notification.enums.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationSender implements NotificationSender {

    private final EmailService emailService;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(Notification notification) {
        if (notification.getRecipientEmail() == null || notification.getRecipientEmail().isBlank()) {
            log.warn("Recipient email is missing for notification {}", notification.getId());
            return;
        }
        emailService.sendHtmlEmail(notification.getRecipientEmail(), notification.getSubject(),
                                   notification.getMessage());
    }
}