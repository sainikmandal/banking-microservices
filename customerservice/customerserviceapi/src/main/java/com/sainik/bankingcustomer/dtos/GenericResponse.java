package com.sainik.bankingcustomer.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenericResponse<T> {

    private String message;
    private T data;
    private boolean success;

    public static <T> GenericResponse<T> success(String message, T data) {
        return new GenericResponse<>(message, data, true);
    }

    public static <T> GenericResponse<T> error(String message) {
        return new GenericResponse<>(message, null, false);
    }
}
