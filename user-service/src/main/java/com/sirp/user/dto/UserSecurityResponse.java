package com.sirp.user.dto;

public record UserSecurityResponse(
        Long id,
        String username,
        String email,
        String password,
        String role,
        Boolean enabled
) {
}