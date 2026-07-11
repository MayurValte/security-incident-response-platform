package com.sirp.notification.feign;

import com.sirp.notification.feign.dto.UserNotificationResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "user-service",
    fallback = UserClientFallback.class
)
public interface UserClient {

    @GetMapping("/internal/users/{id}/notification")
    UserNotificationResponse findNotificationUser(
        @PathVariable UUID id);

}