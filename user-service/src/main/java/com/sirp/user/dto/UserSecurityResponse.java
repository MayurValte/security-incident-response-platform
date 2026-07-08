package com.sirp.user.dto;

import java.util.UUID;

public record UserSecurityResponse(
    UUID id,
    String username,
    String email,
    String password,
    String role,
    Boolean enabled
) {

}