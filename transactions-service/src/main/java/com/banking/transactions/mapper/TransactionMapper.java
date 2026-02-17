package com.banking.transactions.mapper;

import com.banking.transactions.dto.TransactionRequestDto;
import com.banking.transactions.dto.TransactionResponseDto;
import com.banking.transactions.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public Transaction toEntity(TransactionRequestDto dto) {
        return Transaction.builder()
                .accountId(dto.getAccountId())
                .type(dto.getType())
                .amount(dto.getAmount())
                .description(dto.getDescription())
                .build();
    }

    public TransactionResponseDto toDto(Transaction transaction) {
        return TransactionResponseDto.builder()
                .id(transaction.getId())
                .accountId(transaction.getAccountId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .transactionDate(transaction.getTransactionDate())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .referenceNumber(transaction.getReferenceNumber())
                .build();
    }
}
