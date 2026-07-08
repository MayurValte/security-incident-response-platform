package com.sirp.user.controller;

import com.sirp.user.dto.CreateUserRequest;
import com.sirp.user.dto.UpdateUserRequest;
import com.sirp.user.dto.UserResponse;
import com.sirp.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @PostMapping
    public ResponseEntity<UserResponse>
    create(

            @Valid
            @RequestBody
            CreateUserRequest request

    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(request));

    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse>
    get(@PathVariable Long id) {

        return ResponseEntity.ok(
                service.get(id));

    }

    @GetMapping
    public ResponseEntity<Page<UserResponse>>
    getAll(Pageable pageable) {

        return ResponseEntity.ok(
                service.getAll(pageable));

    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse>
    update(

            @PathVariable Long id,

            @RequestBody
            UpdateUserRequest request

    ) {

        return ResponseEntity.ok(

                service.update(id, request)

        );

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void>
    delete(@PathVariable Long id) {

        service.delete(id);

        return ResponseEntity.noContent()

                .build();

    }

}