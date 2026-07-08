package com.sirp.user.dto;

import java.util.UUID;

public record UserNotificationResponse(

    UUID id,

    String username,

    String firstName,

    String lastName,

    String email,

    String phoneNumber,

    boolean enabled

) {

}