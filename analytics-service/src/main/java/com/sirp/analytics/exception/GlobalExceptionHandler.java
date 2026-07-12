package com.sirp.analytics.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception,
        HttpServletRequest request) {

        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                                       .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                       .toList();

        ErrorResponse response = new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(),
                                                   HttpStatus.BAD_REQUEST.getReasonPhrase(), "Validation failed",
                                                   request.getRequestURI(), errors);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception,
        HttpServletRequest request) {

        ErrorResponse response = new ErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(),
                                                   HttpStatus.BAD_REQUEST.getReasonPhrase(),
                                                   "Invalid value for parameter '" + exception.getName() + "'",
                                                   request.getRequestURI(), List.of());

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception, HttpServletRequest request) {

        log.error("Unhandled exception", exception);

        ErrorResponse response = new ErrorResponse(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                                   HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                                                   exception.getMessage(), request.getRequestURI(), List.of());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
