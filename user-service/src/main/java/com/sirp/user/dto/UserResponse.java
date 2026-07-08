package com.sirp.user.dto;

import com.sirp.user.enums.Role;
import java.util.UUID;

public record UserResponse(

    UUID id,

    String username,

    String email,

    Role role,

    String team

) {

}
