package com.sirp.notification.feign;

import com.sirp.notification.feign.dto.UserResponse;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserClientFallback implements UserClient {

    @Override
    public UserResponse getUser(UUID id) {
        log.error("User-Service unavailable while fetching user {}", id);
        return new UserResponse(id, null, null, null, null, null, false);
    }
}