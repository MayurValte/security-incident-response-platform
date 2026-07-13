package com.sirp.workflow.exception;

public class IncidentServiceUnavailableException extends RuntimeException {

    public IncidentServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
