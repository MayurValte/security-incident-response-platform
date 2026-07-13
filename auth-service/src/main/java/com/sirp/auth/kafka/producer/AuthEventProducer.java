package com.sirp.auth.kafka.producer;

import com.sirp.common.events.AuthLoginFailedEvent;
import com.sirp.common.events.AuthLoginSucceededEvent;

public interface AuthEventProducer {

    void publishLoginSucceeded(AuthLoginSucceededEvent event);

    void publishLoginFailed(AuthLoginFailedEvent event);

}
