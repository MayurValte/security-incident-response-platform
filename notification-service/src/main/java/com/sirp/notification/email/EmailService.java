package com.sirp.notification.email;

public interface EmailService {

    void sendHtmlEmail(

        String to,

        String subject,

        String html

                      );

}