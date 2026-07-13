package com.sirp.user.kafka.producer;

import com.sirp.common.events.UserCreatedEvent;
import com.sirp.common.events.UserDeletedEvent;
import com.sirp.common.events.UserUpdatedEvent;

public interface UserEventProducer {

    void publishCreated(UserCreatedEvent event);

    void publishUpdated(UserUpdatedEvent event);

    void publishDeleted(UserDeletedEvent event);

}
