package com.banking.transactions.service;

import com.banking.transactions.dto.TransactionRequestDto;
import com.banking.transactions.dto.TransactionResponseDto;

import java.util.List;

public interface TransactionService {
    TransactionResponseDto processTransaction(TransactionRequestDto dto);
    TransactionResponseDto getTransaction(Long id);
    TransactionResponseDto getTransactionByReference(String referenceNumber);
    List<TransactionResponseDto> getTransactionsByAccount(Long accountId);
    void cancelTransaction(Long id);
}
