package com.sirp.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * No @ComponentScan override needed here or anywhere else in this
 * service - everything from sirp-security is wired in automatically via
 * its AutoConfiguration.imports file.
 *
 * @EnableFeignClients activates UserClient (calls user-service).
 */
@SpringBootApplication
@EnableFeignClients
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}

