package com.sirp.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// UNCHANGED from your uploaded version.
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI authApi() {
        return new OpenAPI().info(new Info().title("SIRP Auth Service")
                                            .version("v1")
                                            .description("Authentication Service")
                                            .contact(new Contact().name("Mayur")));
    }
}
