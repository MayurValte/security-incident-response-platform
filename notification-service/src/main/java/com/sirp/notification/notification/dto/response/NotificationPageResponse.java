package com.sirp.notification.notification.dto.response;

import java.util.List;

public record NotificationPageResponse(

    List<NotificationResponse> content,

    int page,

    int size,

    long totalElements,

    int totalPages,

    boolean first,

    boolean last

) {

}