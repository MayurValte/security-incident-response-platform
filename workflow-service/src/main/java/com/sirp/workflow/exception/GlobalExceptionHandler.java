package com.sirp.workflow.exception;

import com.sirp.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WorkflowNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWorkflowNotFound(WorkflowNotFoundException ex,
        HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(Instant.now(), HttpStatus.NOT_FOUND.value(),
                                                   HttpStatus.NOT_FOUND.getReasonPhrase(), ex.getMessage(),
                                                   request.getRequestURI(), "WORKFLOW_NOT_FOUND", List.of());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(WorkflowAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleWorkflowAlreadyExists(WorkflowAlreadyExistsException ex,
        HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(Instant.now(), HttpStatus.CONFLICT.value(),
                                                   HttpStatus.CONFLICT.getReasonPhrase(), ex.getMessage(),
                                                   request.getRequestURI(), "WORKFLOW_ALREADY_EXISTS", List.of());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(InvalidWorkflowStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidWorkflowState(InvalidWorkflowStateException ex,
        HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(),
                                                   HttpStatus.BAD_REQUEST.getReasonPhrase(), ex.getMessage(),
                                                   request.getRequestURI(), "INVALID_WORKFLOW_STATE", List.of());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IncidentServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleIncidentServiceUnavailable(IncidentServiceUnavailableException ex,
        HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(Instant.now(), HttpStatus.SERVICE_UNAVAILABLE.value(),
                                                   HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(), ex.getMessage(),
                                                   request.getRequestURI(), "INCIDENT_SERVICE_UNAVAILABLE", List.of());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
        HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage)
                                .toList();
        ErrorResponse response = new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(),
                                                   HttpStatus.BAD_REQUEST.getReasonPhrase(), "Validation failed",
                                                   request.getRequestURI(), "VALIDATION_FAILED", errors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
        HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(),
                                                   HttpStatus.BAD_REQUEST.getReasonPhrase(), ex.getMessage(),
                                                   request.getRequestURI(), "CONSTRAINT_VIOLATION", List.of());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                                   HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                                                   ex.getMessage(), request.getRequestURI(), "INTERNAL_SERVER_ERROR",
                                                   List.of());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
