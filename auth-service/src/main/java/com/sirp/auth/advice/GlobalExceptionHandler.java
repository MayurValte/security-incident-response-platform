package com.sirp.auth.advice;

import com.sirp.auth.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * MODIFIED:
 *  - authError(): added earlier, returns 401 for bad login credentials
 *    instead of falling through to the generic 500 handler.
 *  - handle(): now returns a fixed generic message for 500s instead of
 *    ex.getMessage() verbatim, per the production-safety review -
 *    raw exception text can leak internals (SQL fragments, class names,
 *    stack details) to the client. Full exception detail should go to
 *    your logs/observability stack, not the HTTP response body. Wire in
 *    a logger here if you don't already log uncaught exceptions upstream.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> authError(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse response =
                new ApiErrorResponse(
                        LocalDateTime.now(),
                        HttpStatus.UNAUTHORIZED.value(),
                        "Authentication Failed",
                        "Invalid credentials",
                        request.getRequestURI()
                );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handle(
            Exception ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse response =
                new ApiErrorResponse(
                        LocalDateTime.now(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal Server Error",
                        "An unexpected error occurred. Please contact support if this persists.",
                        request.getRequestURI()
                );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> validation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse response =
                new ApiErrorResponse(
                        LocalDateTime.now(),
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
