package com.sirp.notification.config;

import feign.Capability;
import feign.micrometer.MicrometerObservationCapability;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(
    basePackages = "com.sirp.notification.feign"
)
public class FeignConfig {

    /**
     * Spring Cloud OpenFeign 4.3.0 doesn't wire Feign call tracing on
     * its own (confirmed: FeignAutoConfiguration has no Micrometer/
     * Observation bean) - a Capability bean here is what actually gets
     * Feign calls to this service's UserClient propagating trace
     * context and creating client spans.
     */
    @Bean
    Capability micrometerObservationCapability(ObservationRegistry observationRegistry) {
        return new MicrometerObservationCapability(observationRegistry);
    }
}