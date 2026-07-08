package com.sirp.user.dto;

import com.sirp.user.enums.Role;

public record UpdateUserRequest(

        String username,

        String email,

        Role role

) {
}
