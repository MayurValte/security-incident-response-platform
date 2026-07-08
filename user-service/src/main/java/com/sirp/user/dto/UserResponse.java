package com.sirp.user.dto;

import com.sirp.user.enums.Role;

public record UserResponse(

        Long id,

        String username,

        String email,

        Role role,

        String team

) {
}
