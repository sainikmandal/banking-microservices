package com.sainik.bankingcustomer.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerDTO {

    private Long id;

    @NotBlank(message = "First name cannot be blank")
    private String firstName;

    @NotBlank(message = "Last name cannot be blank")
    private String lastName;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Phone cannot be blank")
    @Pattern(regexp = "^\\+?[0-9\\s\\-]+$", message = "Phone must be a valid phone number")
    private String phone;

    @NotBlank(message = "Address cannot be blank")
    private String address;

    private LocalDateTime createdAt;
}
