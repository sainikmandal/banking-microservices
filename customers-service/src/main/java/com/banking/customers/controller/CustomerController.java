package com.banking.customers.controller;

import com.banking.common.dto.ApiResponseDto;
import com.banking.customers.dto.CustomerRequestDto;
import com.banking.customers.dto.CustomerResponseDto;
import com.banking.customers.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "APIs for managing customers")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @Operation(summary = "Register a new customer")
    public ResponseEntity<ApiResponseDto<CustomerResponseDto>> createCustomer(
            @Valid @RequestBody CustomerRequestDto dto) {
        CustomerResponseDto response = customerService.createCustomer(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Customer registered successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<ApiResponseDto<CustomerResponseDto>> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.success(customerService.getCustomer(id)));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get customer by email")
    public ResponseEntity<ApiResponseDto<CustomerResponseDto>> getCustomerByEmail(@PathVariable String email) {
        return ResponseEntity.ok(ApiResponseDto.success(customerService.getCustomerByEmail(email)));
    }

    @GetMapping
    @Operation(summary = "Get all customers")
    public ResponseEntity<ApiResponseDto<List<CustomerResponseDto>>> getAllCustomers() {
        return ResponseEntity.ok(ApiResponseDto.success(customerService.getAllCustomers()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer")
    public ResponseEntity<ApiResponseDto<CustomerResponseDto>> updateCustomer(
            @PathVariable Long id, @Valid @RequestBody CustomerRequestDto dto) {
        return ResponseEntity.ok(ApiResponseDto.success("Customer updated", customerService.updateCustomer(id, dto)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove customer")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
