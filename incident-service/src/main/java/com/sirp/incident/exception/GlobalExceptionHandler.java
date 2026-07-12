package com.sirp.incident.exception;

import com.sirp.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IncidentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleIncidentNotFound(IncidentNotFoundException ex,
        HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(Instant.now(), 404, "Not Found", ex.getMessage(),
                                                   request.getRequestURI(), ErrorCode.INCIDENT_NOT_FOUND.name(),
                                                   List.of());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleTransition(InvalidStatusTransitionException ex,
        HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(Instant.now(), 409, "Conflict", ex.getMessage(),
                                                   request.getRequestURI(), ErrorCode.INVALID_TRANSITION.name(),
                                                   List.of());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage)
                                .toList();
        ErrorResponse response = new ErrorResponse(Instant.now(), 400, "Validation Error", "Request validation failed",
                                                   request.getRequestURI(), ErrorCode.VALIDATION_ERROR.name(), errors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> constraint(ConstraintViolationException ex, HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(Instant.now(), 400, "Validation Error", ex.getMessage(),
                                                   request.getRequestURI(), ErrorCode.VALIDATION_ERROR.name(),
                                                   List.of());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> illegal(IllegalArgumentException ex, HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(Instant.now(), 400, "Bad Request", ex.getMessage(),
                                                   request.getRequestURI(), ErrorCode.INVALID_STATUS.name(),
                                                   List.of());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(UserServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleUserServiceUnavailable(UserServiceUnavailableException ex,
        HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(Instant.now(), 503, "Service Unavailable", ex.getMessage(),
                                                   request.getRequestURI(),
                                                   ErrorCode.USER_SERVICE_UNAVAILABLE.name(), List.of());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> optimistic(OptimisticLockingFailureException ex,
        HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(Instant.now(), 409, "Conflict", "Concurrent modification detected",
                                                   request.getRequestURI(), ErrorCode.INVALID_TRANSITION.name(),
                                                   List.of());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> exception(Exception ex, HttpServletRequest request) {
        ErrorResponse response = new ErrorResponse(Instant.now(), 500, "Internal Server Error", ex.getMessage(),
                                                   request.getRequestURI(), ErrorCode.INTERNAL_SERVER_ERROR.name(),
                                                   List.of());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
