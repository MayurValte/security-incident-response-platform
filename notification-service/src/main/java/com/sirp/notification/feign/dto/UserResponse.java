package com.sirp.notification.feign.dto;

import java.util.UUID;

public record UserResponse(

    UUID id,

    String username,

    String firstName,

    String lastName,

    String email,

    String phoneNumber,

    boolean enabled

) {

}