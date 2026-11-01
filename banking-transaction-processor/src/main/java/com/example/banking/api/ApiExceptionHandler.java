package com.example.banking.api;

import com.example.banking.service.*;
import com.example.banking.domain.InsufficientFundsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AccountNotFoundException.class)
    ResponseEntity<?> notFound(RuntimeException ex) {
        return response(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({AccountAlreadyExistsException.class, InsufficientFundsException.class})
    ResponseEntity<?> conflict(RuntimeException ex) {
        return response(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({InvalidRequestException.class, MethodArgumentNotValidException.class})
    ResponseEntity<?> badRequest(Exception ex) {
        String message = ex instanceof MethodArgumentNotValidException
                ? "Request validation failed" : ex.getMessage();
        return response(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<?> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message));
    }
}
