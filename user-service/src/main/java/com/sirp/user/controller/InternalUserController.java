package com.sirp.user.controller;

import com.sirp.user.dto.UserSecurityResponse;
import com.sirp.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(
        "/internal/users"
)
public class InternalUserController {
    private final UserService service;

    @GetMapping(
            "/email/{email}"
    )
    public UserSecurityResponse findByEmail(
            @PathVariable
            String email
    ) {
        return service.findByEmail(
                email
        );
    }

    @GetMapping(
            "/{id}"
    )
    public UserSecurityResponse findById(
            @PathVariable
            Long id
    ) {
        return service.findSecurityUser(
                id
        );
    }
}