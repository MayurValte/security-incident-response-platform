package com.sirp.notification.email.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @Test
    void sendsHtmlEmailThroughMailSenderOnSuccess() {
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        assertThatCode(() -> emailService.sendHtmlEmail("jdoe@sirp.local", "subject", "<p>hi</p>"))
            .doesNotThrowAnyException();

        verify(mailSender).send(message);
    }

    /**
     * The bug this guards against: sendHtmlEmail() used to catch its own
     * Exception and only log it, so the caller chain never saw a failure
     * and every send - successful or not - was recorded SENT. A send
     * failure MUST propagate so NotificationEventHandlerImpl's
     * dispatchAndRecordOutcome can mark the notification FAILED.
     */
    @Test
    void wrapsAndRethrowsWhenSendFails() {
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        MailSendException sendFailure = new MailSendException("SMTP connection refused");
        doThrow(sendFailure).when(mailSender).send(message);

        assertThatThrownBy(() -> emailService.sendHtmlEmail("jdoe@sirp.local", "subject", "<p>hi</p>"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("jdoe@sirp.local")
            .hasCause(sendFailure);
    }
}
