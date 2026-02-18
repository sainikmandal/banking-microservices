package com.sainik.bankingaccountapi.exceptions;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.sainik.bankingaccountapi.dtos.GenericResponse;

@ControllerAdvice
public class GlobalException {

    // 1. Handle Resource Not Found (404)
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<GenericResponse<String>> handleAccountNotFoundException(AccountNotFoundException ex) {
        // Returns 404 Not Found
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new GenericResponse<>(ex.getMessage()));
    }

    // 2. Handle Business Logic Errors (400)
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<GenericResponse<String>> handleInsufficientBalanceException(InsufficientBalanceException ex) {
        // Returns 400 Bad Request
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericResponse<>(ex.getMessage()));
    }

    // 3. Handle Data Conflicts (409)
    @ExceptionHandler(AccountAlreadyExistsException.class)
    public ResponseEntity<GenericResponse<String>> handleAccountAlreadyExistsException(AccountAlreadyExistsException ex) {
        // Returns 409 Conflict
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new GenericResponse<>(ex.getMessage()));
    }

    // 4. Handle Validation Errors (400) - e.g. @NotNull, @Size failures
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse<Map<String,Object>>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        Map<String, Object> response = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        });

        response.put("errors", fieldErrors);
        response.put("message", "Validation failed");
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericResponse<>(response));
    }

    // 5. Handle Generic/Unknown Errors (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse<String>> handleException(Exception ex) {
        // SECURITY: We do NOT pass ex.getMessage() here to avoid exposing internal logic
        // We return a generic message instead.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>("An internal server error occurred. Please contact support."));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<GenericResponse<String>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>("An internal runtime error occurred."));
    }
}