package com.banking.transactions.controller;

import com.banking.common.dto.ApiResponseDto;
import com.banking.transactions.dto.TransactionRequestDto;
import com.banking.transactions.dto.TransactionResponseDto;
import com.banking.transactions.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Management", description = "APIs for managing transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Process a new transaction")
    public ResponseEntity<ApiResponseDto<TransactionResponseDto>> processTransaction(
            @Valid @RequestBody TransactionRequestDto dto) {
        TransactionResponseDto response = transactionService.processTransaction(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Transaction processed", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<ApiResponseDto<TransactionResponseDto>> getTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.success(transactionService.getTransaction(id)));
    }

    @GetMapping("/reference/{referenceNumber}")
    @Operation(summary = "Get transaction by reference number")
    public ResponseEntity<ApiResponseDto<TransactionResponseDto>> getTransactionByReference(
            @PathVariable String referenceNumber) {
        return ResponseEntity.ok(ApiResponseDto.success(transactionService.getTransactionByReference(referenceNumber)));
    }

    @GetMapping
    @Operation(summary = "Get transactions by account")
    public ResponseEntity<ApiResponseDto<List<TransactionResponseDto>>> getTransactionsByAccount(
            @RequestParam Long accountId) {
        return ResponseEntity.ok(ApiResponseDto.success(transactionService.getTransactionsByAccount(accountId)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a pending transaction")
    public ResponseEntity<Void> cancelTransaction(@PathVariable Long id) {
        transactionService.cancelTransaction(id);
        return ResponseEntity.noContent().build();
    }
}
