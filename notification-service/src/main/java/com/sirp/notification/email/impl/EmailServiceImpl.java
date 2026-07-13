package com.sirp.notification.email.impl;

import com.sirp.notification.email.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendHtmlEmail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("HTML email sent to {}", to);
        } catch (Exception ex) {
            // MUST propagate, not just log: the caller chain
            // (EmailNotificationSender -> NotificationDispatcherImpl ->
            // NotificationEventHandlerImpl.dispatchAndRecordOutcome) relies
            // on an exception here to mark the notification FAILED and
            // record failureReason. Swallowing it meant every send
            // attempt - success or failure - ended up recorded as SENT,
            // so a real SMTP outage silently dropped notifications with
            // no trace, and NotificationRetryScheduler would never see
            // rows to retry in the first place.
            throw new IllegalStateException("Failed to send email to " + to, ex);
        }
    }
}