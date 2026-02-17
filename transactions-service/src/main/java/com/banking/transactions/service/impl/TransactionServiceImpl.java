package com.banking.transactions.service.impl;

import com.banking.common.exception.BusinessException;
import com.banking.common.exception.ResourceNotFoundException;
import com.banking.transactions.client.AccountServiceClient;
import com.banking.transactions.dto.TransactionRequestDto;
import com.banking.transactions.dto.TransactionResponseDto;
import com.banking.transactions.entity.Transaction;
import com.banking.transactions.entity.TransactionStatus;
import com.banking.transactions.entity.TransactionType;
import com.banking.transactions.mapper.TransactionMapper;
import com.banking.transactions.repository.TransactionRepository;
import com.banking.transactions.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final AccountServiceClient accountServiceClient;

    @Override
    @Transactional
    public TransactionResponseDto processTransaction(TransactionRequestDto dto) {
        // Validate account exists
        accountServiceClient.getAccount(dto.getAccountId());

        // Create transaction with PENDING status
        Transaction transaction = transactionMapper.toEntity(dto);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setReferenceNumber(UUID.randomUUID().toString());
        transaction = transactionRepository.save(transaction);

        try {
            // Update account balance
            Map<String, Object> balanceUpdate = new HashMap<>();
            balanceUpdate.put("amount", dto.getAmount());

            if (dto.getType() == TransactionType.DEPOSIT) {
                balanceUpdate.put("operationType", "DEPOSIT");
            } else if (dto.getType() == TransactionType.WITHDRAWAL) {
                balanceUpdate.put("operationType", "WITHDRAWAL");
            } else {
                balanceUpdate.put("operationType", "WITHDRAWAL");
            }

            accountServiceClient.updateBalance(dto.getAccountId(), balanceUpdate);

            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction = transactionRepository.save(transaction);
            log.info("Transaction completed: {}", transaction.getReferenceNumber());

        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            log.error("Transaction failed: {}", transaction.getReferenceNumber(), e);
            throw new BusinessException("Transaction processing failed: " + e.getMessage());
        }

        return transactionMapper.toDto(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponseDto getTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
        return transactionMapper.toDto(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponseDto getTransactionByReference(String referenceNumber) {
        Transaction transaction = transactionRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "referenceNumber", referenceNumber));
        return transactionMapper.toDto(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getTransactionsByAccount(Long accountId) {
        return transactionRepository.findByAccountId(accountId).stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new BusinessException("Only pending transactions can be cancelled");
        }

        transaction.setStatus(TransactionStatus.CANCELLED);
        transactionRepository.save(transaction);
        log.info("Transaction cancelled: {}", transaction.getReferenceNumber());
    }
}
