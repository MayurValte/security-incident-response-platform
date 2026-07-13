package com.sirp.incident.config;

import feign.Capability;
import feign.Logger;
import feign.micrometer.MicrometerObservationCapability;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    Logger.Level feignLoggerLevel() {

        return Logger.Level.FULL;

    }

    @Bean
    Capability micrometerObservationCapability(ObservationRegistry observationRegistry) {
        return new MicrometerObservationCapability(observationRegistry);
    }

}