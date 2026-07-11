package com.sirp.workflow.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignWorkflowRequest(

    @NotNull(message = "Assigned user is required")
    UUID assignedTo,

    UUID assignedTeam

) {

}
