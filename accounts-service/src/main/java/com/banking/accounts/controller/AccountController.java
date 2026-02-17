package com.banking.accounts.controller;

import com.banking.accounts.dto.AccountRequestDto;
import com.banking.accounts.dto.AccountResponseDto;
import com.banking.accounts.dto.BalanceUpdateDto;
import com.banking.accounts.service.AccountService;
import com.banking.common.dto.ApiResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Account Management", description = "APIs for managing bank accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Create a new account")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<ApiResponseDto<AccountResponseDto>> createAccount(
            @Valid @RequestBody AccountRequestDto dto) {
        AccountResponseDto response = accountService.createAccount(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Account created successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<ApiResponseDto<AccountResponseDto>> getAccount(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.success(accountService.getAccount(id)));
    }

    @GetMapping("/number/{accountNumber}")
    @Operation(summary = "Get account by account number")
    public ResponseEntity<ApiResponseDto<AccountResponseDto>> getAccountByNumber(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(ApiResponseDto.success(accountService.getAccountByNumber(accountNumber)));
    }

    @GetMapping
    @Operation(summary = "Get accounts by customer ID")
    public ResponseEntity<ApiResponseDto<List<AccountResponseDto>>> getAccountsByCustomer(
            @RequestParam Long customerId) {
        return ResponseEntity.ok(ApiResponseDto.success(accountService.getAccountsByCustomer(customerId)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update account")
    public ResponseEntity<ApiResponseDto<AccountResponseDto>> updateAccount(
            @PathVariable Long id, @Valid @RequestBody AccountRequestDto dto) {
        return ResponseEntity.ok(ApiResponseDto.success("Account updated", accountService.updateAccount(id, dto)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Close account")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/balance")
    @Operation(summary = "Update account balance (deposit/withdrawal)")
    public ResponseEntity<ApiResponseDto<AccountResponseDto>> updateBalance(
            @PathVariable Long id, @Valid @RequestBody BalanceUpdateDto dto) {
        return ResponseEntity.ok(ApiResponseDto.success(accountService.updateBalance(id, dto)));
    }
}
