package com.sirp.notification.email.model;

import java.time.Instant;

public record IncidentEmailModel(

    String incidentNumber,

    String title,

    String description,

    String priority,

    String severity,

    String status,

    String createdBy,

    Instant createdAt,

    String incidentUrl

) {

}