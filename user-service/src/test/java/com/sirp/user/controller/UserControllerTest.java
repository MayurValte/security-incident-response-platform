package com.sirp.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirp.security.model.JwtUser;
import com.sirp.user.dto.CreateUserRequest;
import com.sirp.user.dto.UpdateUserRequest;
import com.sirp.user.dto.UserResponse;
import com.sirp.user.enums.Role;
import com.sirp.user.exception.ResourceNotFoundException;
import com.sirp.user.service.UserService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import com.sirp.user.exception.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService service;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID actorId;
    private JwtUser principal;

    @BeforeEach
    void setUp() {
        UserController controller = new UserController(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(new LocalValidatorFactoryBean())
            .setCustomArgumentResolvers(
                new AuthenticationPrincipalArgumentResolver(),
                new PageableHandlerMethodArgumentResolver())
            .build();

        actorId = UUID.randomUUID();
        principal = new JwtUser(actorId, "admin@sirp.local", "ADMIN", null);
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(principal, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_returns201WithCreatedUserAndActorFromPrincipal() throws Exception {
        UUID teamId = UUID.randomUUID();
        CreateUserRequest request = new CreateUserRequest("jdoe", "jdoe@sirp.local", "Passw0rd!",
            Role.ENGINEER, teamId);
        UUID newUserId = UUID.randomUUID();
        UserResponse response = new UserResponse(newUserId, "jdoe", "jdoe@sirp.local", Role.ENGINEER,
            "Bootstrap Team");
        when(service.create(eq(request), eq(actorId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/users")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(newUserId.toString()))
            .andExpect(jsonPath("$.username").value("jdoe"))
            .andExpect(jsonPath("$.team").value("Bootstrap Team"));

        verify(service).create(eq(request), eq(actorId));
    }

    @Test
    void create_returns400WhenPasswordFailsComplexityRule() throws Exception {
        CreateUserRequest request = new CreateUserRequest("jdoe", "jdoe@sirp.local", "weak",
            Role.ENGINEER, UUID.randomUUID());

        mockMvc.perform(post("/api/v1/users")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void get_returns200WithUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse response = new UserResponse(userId, "jdoe", "jdoe@sirp.local", Role.ENGINEER,
            "Bootstrap Team");
        when(service.get(userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/{id}", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId.toString()));
    }

    @Test
    void get_returns404WhenUserMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        when(service.get(userId)).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/v1/users/{id}", userId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getAll_returns200WithPageContent() throws Exception {
        UserResponse response = new UserResponse(UUID.randomUUID(), "jdoe", "jdoe@sirp.local", Role.ENGINEER,
            "Bootstrap Team");
        when(service.getAll(any())).thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].username").value("jdoe"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void update_returns200WithUpdatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserRequest request = new UpdateUserRequest("jdoe2", "jdoe2@sirp.local", Role.MANAGER);
        UserResponse response = new UserResponse(userId, "jdoe2", "jdoe2@sirp.local", Role.MANAGER,
            "Bootstrap Team");
        when(service.update(eq(userId), eq(request), eq(actorId))).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/{id}", userId)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("jdoe2"))
            .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    void update_returns404WhenUserMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserRequest request = new UpdateUserRequest("jdoe2", "jdoe2@sirp.local", Role.MANAGER);
        when(service.update(eq(userId), eq(request), eq(actorId)))
            .thenThrow(new ResourceNotFoundException("User not found with id : " + userId));

        mockMvc.perform(put("/api/v1/users/{id}", userId)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204AndUsesActorFromPrincipal() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/users/{id}", userId))
            .andExpect(status().isNoContent());

        verify(service).delete(userId, actorId);
    }

    @Test
    void delete_returns404WhenUserMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("User not found with id : " + userId))
            .when(service).delete(userId, actorId);

        mockMvc.perform(delete("/api/v1/users/{id}", userId))
            .andExpect(status().isNotFound());
    }
}
