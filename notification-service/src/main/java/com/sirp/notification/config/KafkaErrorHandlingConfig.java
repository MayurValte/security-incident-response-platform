package com.sirp.notification.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
public class KafkaErrorHandlingConfig {

    private static final String SERVICE_NAME = "notification-service";

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        // Per-consuming-service DLT suffix (<topic>.<service>.DLT) rather
        // than Spring Kafka's bare default (<topic>.DLT), because the
        // incident.* topics this service listens on are also consumed by
        // analytics-service and audit-service - a shared .DLT topic
        // would mix failures from all three with no way to tell which
        // service's processing actually failed.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + "." + SERVICE_NAME + ".DLT", record.partition()));

        // 1 initial attempt + 3 retries, doubling each time: 1s, 2s, 4s
        // (capped at maxInterval) before giving up and dead-lettering.
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000L);

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
