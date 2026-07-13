package com.sirp.user.kafka.producer;

import com.sirp.common.events.UserCreatedEvent;
import com.sirp.common.events.UserDeletedEvent;
import com.sirp.common.events.UserUpdatedEvent;
import com.sirp.common.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventProducerImpl implements UserEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishCreated(UserCreatedEvent event) {
        kafkaTemplate.send(KafkaTopics.USER_CREATED, event.userId().toString(), event);
        log.info("Published UserCreatedEvent [{}]", event.eventId());
    }

    @Override
    public void publishUpdated(UserUpdatedEvent event) {
        kafkaTemplate.send(KafkaTopics.USER_UPDATED, event.userId().toString(), event);
        log.info("Published UserUpdatedEvent [{}]", event.eventId());
    }

    @Override
    public void publishDeleted(UserDeletedEvent event) {
        kafkaTemplate.send(KafkaTopics.USER_DELETED, event.userId().toString(), event);
        log.info("Published UserDeletedEvent [{}]", event.eventId());
    }
}
