package com.sainik.bankingcustomer.exceptions;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.sainik.bankingcustomer.dtos.GenericResponse;

@ControllerAdvice
public class GlobalException {

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<GenericResponse<String>> handleCustomerNotFoundException(CustomerNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new GenericResponse<>(ex.getMessage()));
    }

    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public ResponseEntity<GenericResponse<String>> handleCustomerAlreadyExistsException(CustomerAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new GenericResponse<>(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse<Map<String, Object>>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse<String>> handleException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>("An internal server error occurred. Please contact support."));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<GenericResponse<String>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>("An internal runtime error occurred."));
    }
}
