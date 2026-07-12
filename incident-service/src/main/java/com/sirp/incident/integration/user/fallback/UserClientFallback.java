package com.sirp.incident.integration.user.fallback;

import com.sirp.incident.exception.UserServiceUnavailableException;
import com.sirp.incident.integration.user.UserClient;
import com.sirp.incident.integration.user.dto.UserResponse;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UserClientFallback implements UserClient {

    @Override
    public UserResponse getUser(UUID id) {
        throw new UserServiceUnavailableException("User Service unavailable");
    }
}