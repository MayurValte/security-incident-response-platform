package com.sirp.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "auth.login-throttle")
public class LoginThrottleProperties {

    private int maxAttempts = 5;
    private long attemptWindowSeconds = 900;
    private long lockoutSeconds = 900;
}
