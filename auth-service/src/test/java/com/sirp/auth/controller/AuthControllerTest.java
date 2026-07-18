package com.sirp.auth.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirp.auth.advice.GlobalExceptionHandler;
import com.sirp.auth.dto.request.LoginRequest;
import com.sirp.auth.dto.request.RefreshTokenRequest;
import com.sirp.auth.dto.response.AuthResponse;
import com.sirp.auth.exception.AccountLockedException;
import com.sirp.auth.service.AuthService;
import com.sirp.security.model.JwtUser;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private UUID userId;
    private JwtUser principal;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(new LocalValidatorFactoryBean())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();

        userId = UUID.randomUUID();
        principal = new JwtUser(userId, "jdoe@sirp.local", "ENGINEER", null);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(principal, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void login_returns200WithTokensOnSuccess() throws Exception {
        LoginRequest request = new LoginRequest("jdoe@sirp.local", "Passw0rd!");
        AuthResponse response = new AuthResponse("access-token", "refresh-token", "Bearer", 900000L);
        when(authService.login(eq(request))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_returns400WhenEmailMalformed() throws Exception {
        LoginRequest request = new LoginRequest("not-an-email", "Passw0rd!");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void login_returns401WhenCredentialsInvalid() throws Exception {
        LoginRequest request = new LoginRequest("jdoe@sirp.local", "wrong-password");
        when(authService.login(eq(request))).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void login_returns423WhenAccountLocked() throws Exception {
        LoginRequest request = new LoginRequest("jdoe@sirp.local", "Passw0rd!");
        when(authService.login(eq(request))).thenThrow(new AccountLockedException("locked out"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isLocked())
            .andExpect(jsonPath("$.error").value("Account Locked"));
    }

    @Test
    void refresh_returns200WithRotatedTokens() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("old-refresh-token");
        AuthResponse response = new AuthResponse("new-access-token", "new-refresh-token", "Bearer", 900000L);
        when(authService.refresh(eq(request))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    void refresh_returns401WhenTokenInvalid() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("bad-token");
        when(authService.refresh(eq(request))).thenThrow(new BadCredentialsException("Refresh token not found"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_returns204AndUsesUserIdFromPrincipal() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
            .andExpect(status().isNoContent());

        verify(authService).logout(userId);
    }
}
