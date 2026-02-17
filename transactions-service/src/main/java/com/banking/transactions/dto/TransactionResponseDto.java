package com.banking.transactions.dto;

import com.banking.transactions.entity.TransactionStatus;
import com.banking.transactions.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDto {
    private Long id;
    private Long accountId;
    private TransactionType type;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private TransactionStatus status;
    private String description;
    private String referenceNumber;
}
