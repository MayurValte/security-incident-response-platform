package com.sirp.incident.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declaring this bean at all replaces Spring Boot's autoconfigured
 * ObjectMapper (JacksonAutoConfiguration backs off once any ObjectMapper
 * bean exists), which otherwise disables FAIL_ON_UNKNOWN_PROPERTIES by
 * default - vanilla Jackson enables it. Without this, any Feign response
 * carrying a field this service's DTOs don't declare (e.g. UserClient's
 * UserResponse against user-service's actual payload, which also
 * includes "password") fails to decode at all instead of just ignoring
 * the extra field.
 */
@Configuration
public class JacksonConfig {

  @Bean
  ObjectMapper objectMapper() {
    return new ObjectMapper()
        .findAndRegisterModules()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }
}