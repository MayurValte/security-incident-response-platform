package com.sirp.workflow.config;

import feign.Capability;
import feign.micrometer.MicrometerObservationCapability;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cloud OpenFeign 4.3.0 doesn't wire Feign call tracing on its
 * own (confirmed: FeignAutoConfiguration has no Micrometer/Observation
 * bean) - a Capability bean here is what actually gets Feign calls to
 * this service's IncidentServiceClient propagating trace context and
 * creating client spans. Declaring it as a top-level @Bean applies it
 * to every Feign client in this service.
 */
@Configuration
public class FeignTracingConfig {

    @Bean
    public Capability micrometerObservationCapability(ObservationRegistry observationRegistry) {
        return new MicrometerObservationCapability(observationRegistry);
    }
}
