package com.sirp.audit.config;

import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
public class KafkaErrorHandlingConfig {

    private static final String SERVICE_NAME = "audit-service";

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        // Per-consuming-service DLT suffix (<topic>.<service>.DLT) rather
        // than Spring Kafka's bare default (<topic>.DLT), because the
        // incident.* topics this service listens on are also consumed by
        // notification-service and analytics-service - a shared .DLT
        // topic would mix failures from all three with no way to tell
        // which service's processing actually failed. (The workflow.*
        // topics are only consumed here, so this only matters for the
        // incident.* half of this consumer's subscriptions - kept
        // uniform across all 9 listeners for consistency regardless.)
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

    /**
     * Correlation ID = the current record's Micrometer trace ID (see
     * sirp-security's CorrelationIdFilter for the HTTP-side equivalent
     * and the reasoning against building a second ID scheme) - Spring
     * Kafka's observation instrumentation (spring.kafka.listener.
     * observation-enabled: true) already puts "traceId" into MDC before
     * the listener method runs, this just mirrors it under an explicit
     * "correlationId" key so grep-by-name works the same way across the
     * HTTP and Kafka sides. Auto-applied to every @KafkaListener in this
     * service the same way the DefaultErrorHandler bean above is.
     */
    @Bean
    public RecordInterceptor<Object, Object> correlationIdRecordInterceptor() {
        return new RecordInterceptor<>() {
            @Override
            public ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> record,
                Consumer<Object, Object> consumer) {
                String correlationId = MDC.get("traceId");
                MDC.put("correlationId", (correlationId == null || correlationId.isBlank())
                    ? UUID.randomUUID().toString() : correlationId);
                return record;
            }

            @Override
            public void afterRecord(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
                MDC.remove("correlationId");
            }
        };
    }
}
