package com.sirp.user.exception;

import java.util.UUID;

public class TeamInUseException extends RuntimeException {

    public TeamInUseException(UUID teamId) {

        super("Cannot delete team because users are assigned to it. Team id : " + teamId);

    }

}