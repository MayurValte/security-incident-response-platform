package com.sirp.common.events;

import java.time.LocalDateTime;

public record UserCreatedEvent(

        Long userId,

        String username,

        String email,

        LocalDateTime createdAt

) {
}