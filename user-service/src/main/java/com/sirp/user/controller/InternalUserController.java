package com.sirp.user.controller;

import com.sirp.user.dto.UserNotificationResponse;
import com.sirp.user.dto.UserSecurityResponse;
import com.sirp.user.service.UserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserService service;

    @GetMapping("/email/{email}")
    public UserSecurityResponse findByEmail(@PathVariable String email) {
        return service.findByEmail(email);
    }

    @GetMapping("/{id}")
    public UserSecurityResponse findById(@PathVariable UUID id) {
        return service.findSecurityUser(id);
    }

    @GetMapping("/{id}/notification")
    public UserNotificationResponse findNotificationUser(@PathVariable UUID id) {
        return service.findNotificationUser(id);
    }
}