package com.sirp.auth.advice;

import com.sirp.auth.exception.UserServiceUnavailableException;
import com.sirp.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * authError(): returns 401 for bad login credentials instead of falling
 * through to the generic 500 handler.
 *
 * handle(): returns a fixed generic message for 500s instead of
 * ex.getMessage() verbatim, per the production-safety review - raw
 * exception text can leak internals (SQL fragments, class names, stack
 * details) to the client. Full exception detail should go to your
 * logs/observability stack, not the HTTP response body. Wire in a
 * logger here if you don't already log uncaught exceptions upstream.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> authError(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        ErrorResponse response =
                new ErrorResponse(
                        Instant.now(),
                        HttpStatus.UNAUTHORIZED.value(),
                        "Authentication Failed",
                        "Invalid credentials",
                        request.getRequestURI()
                );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(UserServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> userServiceUnavailable(
            UserServiceUnavailableException ex,
            HttpServletRequest request
    ) {
        ErrorResponse response =
                new ErrorResponse(
                        Instant.now(),
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "Service Unavailable",
                        "User service is temporarily unavailable. Please try again shortly.",
                        request.getRequestURI()
                );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handle(
            Exception ex,
            HttpServletRequest request
    ) {
        ErrorResponse response =
                new ErrorResponse(
                        Instant.now(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal Server Error",
                        "An unexpected error occurred. Please contact support if this persists.",
                        request.getRequestURI()
                );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        ErrorResponse response =
                new ErrorResponse(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        "Validation Error",
                        ex.getBindingResult().getFieldError() == null
                            ? "Validation failed"
                            : ex.getBindingResult().getFieldError().getDefaultMessage(),
                        request.getRequestURI()
                );
        return ResponseEntity.badRequest().body(response);
    }
}
