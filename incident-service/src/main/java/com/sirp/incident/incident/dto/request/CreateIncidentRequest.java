package com.sirp.incident.incident.dto.request;

import com.sirp.common.enums.IncidentPriority;
import com.sirp.common.enums.IncidentSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateIncidentRequest(

    @NotBlank(message = "Title is required")
    @Size(max = 255)
    String title,

    @NotBlank(message = "Description is required")
    @Size(max = 5000)
    String description,

    @NotNull
    IncidentSeverity severity,

    @NotNull
    IncidentPriority priority,

    UUID teamId

) {

}