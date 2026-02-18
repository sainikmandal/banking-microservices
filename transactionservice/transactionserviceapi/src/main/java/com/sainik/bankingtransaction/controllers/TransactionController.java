package com.sainik.bankingtransaction.controllers;

import com.sainik.bankingtransaction.dtos.GenericResponse;
import com.sainik.bankingtransaction.dtos.TransactionDTO;
import com.sainik.bankingtransaction.services.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Transaction Management", description = "APIs for managing banking transactions")
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * POST /transactions/v1.0 — Initiate a new transaction
     * Requires SCOPE_developer
     */
    @PostMapping("/v1.0")
    @PreAuthorize("hasAnyAuthority('SCOPE_developer')")
    @Operation(
            summary = "Initiate a transaction",
            description = "Create a new banking transaction. Amount must be positive.",
            security = @SecurityRequirement(name = "oauth2")
    )
    public ResponseEntity<GenericResponse<TransactionDTO>> createTransaction(
            @Valid @RequestBody TransactionDTO transactionDTO) {
        TransactionDTO created = transactionService.createTransaction(transactionDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GenericResponse.success("Transaction initiated successfully", created));
    }

    /**
     * GET /transactions/v1.0 — Get all transactions
     */
    @GetMapping("/v1.0")
    @Operation(
            summary = "Get all transactions",
            description = "Retrieve all transactions",
            security = @SecurityRequirement(name = "oauth2")
    )
    public ResponseEntity<GenericResponse<List<TransactionDTO>>> getAllTransactions() {
        List<TransactionDTO> transactions = transactionService.getAllTransactions();
        return ResponseEntity.ok(GenericResponse.success("Transactions retrieved successfully", transactions));
    }

    /**
     * GET /transactions/v1.0/{id} — Get transaction by ID
     */
    @GetMapping("/v1.0/{id}")
    @Operation(
            summary = "Get transaction by ID",
            description = "Retrieve a specific transaction by its ID",
            security = @SecurityRequirement(name = "oauth2")
    )
    public ResponseEntity<GenericResponse<TransactionDTO>> getTransactionById(@PathVariable Long id) {
        TransactionDTO transaction = transactionService.getTransactionById(id);
        return ResponseEntity.ok(GenericResponse.success("Transaction retrieved successfully", transaction));
    }

    /**
     * GET /transactions/v1.0/account/{accountId} — Get all transactions for an account
     */
    @GetMapping("/v1.0/account/{accountId}")
    @Operation(
            summary = "Get transactions by account ID",
            description = "Retrieve all transactions associated with a specific account",
            security = @SecurityRequirement(name = "oauth2")
    )
    public ResponseEntity<GenericResponse<List<TransactionDTO>>> getTransactionsByAccountId(
            @PathVariable Long accountId) {
        List<TransactionDTO> transactions = transactionService.getTransactionsByAccountId(accountId);
        return ResponseEntity.ok(GenericResponse.success("Transactions for account retrieved successfully", transactions));
    }

    /**
     * PUT /transactions/v1.0/{id} — Amend a PENDING transaction
     * Requires SCOPE_developer
     */
    @PutMapping("/v1.0/{id}")
    @PreAuthorize("hasAnyAuthority('SCOPE_developer')")
    @Operation(
            summary = "Amend a transaction",
            description = "Update a PENDING transaction. Cannot amend SUCCESS or FAILED transactions.",
            security = @SecurityRequirement(name = "oauth2")
    )
    public ResponseEntity<GenericResponse<TransactionDTO>> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionDTO transactionDTO) {
        TransactionDTO updated = transactionService.updateTransaction(id, transactionDTO);
        return ResponseEntity.ok(GenericResponse.success("Transaction updated successfully", updated));
    }

    /**
     * DELETE /transactions/v1.0/{id} — Cancel a PENDING transaction
     * Requires SCOPE_developer
     */
    @DeleteMapping("/v1.0/{id}")
    @PreAuthorize("hasAnyAuthority('SCOPE_developer')")
    @Operation(
            summary = "Cancel a transaction",
            description = "Cancel/delete a PENDING transaction. Cannot cancel SUCCESS or FAILED transactions.",
            security = @SecurityRequirement(name = "oauth2")
    )
    public ResponseEntity<GenericResponse<Object>> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok(GenericResponse.success("Transaction cancelled successfully", null));
    }
}
