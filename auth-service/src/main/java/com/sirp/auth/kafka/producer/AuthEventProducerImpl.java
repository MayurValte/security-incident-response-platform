package com.sirp.auth.kafka.producer;

import com.sirp.common.events.AuthLoginFailedEvent;
import com.sirp.common.events.AuthLoginSucceededEvent;
import com.sirp.common.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventProducerImpl implements AuthEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishLoginSucceeded(AuthLoginSucceededEvent event) {
        kafkaTemplate.send(KafkaTopics.AUTH_LOGIN_SUCCEEDED, event.userId().toString(), event);
        log.info("Published AuthLoginSucceededEvent [{}]", event.eventId());
    }

    @Override
    public void publishLoginFailed(AuthLoginFailedEvent event) {
        kafkaTemplate.send(KafkaTopics.AUTH_LOGIN_FAILED, event.email(), event);
        log.info("Published AuthLoginFailedEvent [{}]", event.eventId());
    }
}
