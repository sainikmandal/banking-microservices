package com.sainik.bankingtransaction.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Transaction data transfer object")
public class TransactionDTO {

    @Schema(description = "Transaction ID (auto-generated)", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull(message = "Account ID is required")
    @Schema(description = "ID of the account this transaction belongs to", example = "1")
    private Long accountId;

    @NotBlank(message = "Transaction type is required")
    @Pattern(regexp = "^(Deposit|Withdrawal|Transfer)$", message = "Type must be Deposit, Withdrawal, or Transfer")
    @Schema(description = "Transaction type", example = "Deposit", allowableValues = {"Deposit", "Withdrawal", "Transfer"})
    private String type;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @Schema(description = "Transaction amount (must be positive)", example = "250.00")
    private BigDecimal amount;

    @Schema(description = "Transaction date (auto-set on creation)", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime transactionDate;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(SUCCESS|FAILED|PENDING)$", message = "Status must be SUCCESS, FAILED, or PENDING")
    @Schema(description = "Transaction status", example = "PENDING", allowableValues = {"SUCCESS", "FAILED", "PENDING"})
    private String status;
}
