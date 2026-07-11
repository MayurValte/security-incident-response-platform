package com.sirp.auth.feign;

import com.sirp.auth.dto.response.UserSecurityResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * MODIFIED - paths now match your actual InternalUserController
 * (@RequestMapping("/internal/users")):
 *   GET /internal/users/email/{email}
 *   GET /internal/users/{id}
 *
 * "user-service" must match that service's spring.application.name for
 * discovery-based routing (Consul) to resolve it.
 *
 * IMPORTANT: com.sirp.auth.dto.response.UserSecurityResponse (id, email,
 * password, role, enabled) must have the same JSON shape as
 * com.sirp.user.dto.UserSecurityResponse on the user-service side, or
 * Feign's Jackson deserialization will silently drop/null out any
 * mismatched fields instead of failing loudly. Please confirm those
 * field names line up - if user-service's version differs, share it and
 * I'll adjust this DTO to match rather than the other way around, since
 * user-service already owns that contract.
 */
@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/internal/users/email/{email}")
    UserSecurityResponse findByEmail(@PathVariable("email") String email);

    @GetMapping("/internal/users/{id}")
    UserSecurityResponse findById(@PathVariable("id") UUID id);
}

