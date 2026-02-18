package com.sainik.bankingtransaction.exceptions;

import com.sainik.bankingtransaction.dtos.GenericResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalException {

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<GenericResponse<Object>> handleTransactionNotFound(TransactionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(GenericResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<GenericResponse<Object>> handleInvalidTransaction(InvalidTransactionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GenericResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse<Map<String, String>>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(fieldName, message);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericResponse<>("Validation failed", errors, false));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse<Object>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GenericResponse.error("An unexpected error occurred: " + ex.getMessage()));
    }
}
