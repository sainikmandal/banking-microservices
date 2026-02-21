package com.sainik.bankingtransaction.services;

import com.sainik.bankingtransaction.client.AccountServiceClient;
import com.sainik.bankingtransaction.dtos.TransactionDTO;
import com.sainik.bankingtransaction.exceptions.InvalidTransactionException;
import com.sainik.bankingtransaction.exceptions.TransactionNotFoundException;
import com.sainik.bankingtransaction.mappers.TransactionMapper;
import com.sainik.bankingtransaction.models.Transaction;
import com.sainik.bankingtransaction.repositories.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final AccountServiceClient accountServiceClient;

    /**
     * Initiate (create) a new transaction.
     *
     * Status is determined entirely by the server — never trusted from the client:
     *   - Calls account-service via RestClient to check the account exists
     *   - Account found    → status = SUCCESS
     *   - Account missing  → status = FAILED  (still persisted for audit trail)
     *   - Network/error    → status = FAILED  (AccountServiceClient swallows the error)
     */
    public TransactionDTO createTransaction(TransactionDTO dto) {
        log.info("Creating transaction for accountId={}, type={}, amount={}", dto.getAccountId(), dto.getType(), dto.getAmount());

        // Guard: amount must be positive
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transaction amount must be positive");
        }

        // Validate account exists via inter-service RestClient call
        boolean accountExists = accountServiceClient.accountExists(dto.getAccountId());
        String resolvedStatus = accountExists ? "SUCCESS" : "FAILED";
        log.info("Account existence check for accountId={}: {} → status={}", dto.getAccountId(), accountExists, resolvedStatus);

        Transaction transaction = transactionMapper.toEntity(dto);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus(resolvedStatus); // always overwrite — client input is ignored

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction ID={} persisted with status={}", saved.getId(), saved.getStatus());
        return transactionMapper.toDTO(saved);
    }

    /**
     * Get all transactions.
     */
    public List<TransactionDTO> getAllTransactions() {
        return transactionRepository.findAll()
                .stream()
                .map(transactionMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a transaction by ID.
     */
    public TransactionDTO getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        return transactionMapper.toDTO(transaction);
    }

    /**
     * Get all transactions for a given account.
     */
    public List<TransactionDTO> getTransactionsByAccountId(Long accountId) {
        List<Transaction> transactions = transactionRepository.findByAccountId(accountId);
        if (transactions.isEmpty()) {
            log.warn("No transactions found for accountId={}", accountId);
        }
        return transactions.stream()
                .map(transactionMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Amend (update) a transaction — only allowed for PENDING transactions.
     * Business rules:
     *   - Cannot amend a SUCCESS or FAILED transaction
     *   - New amount must be positive
     */
    public TransactionDTO updateTransaction(Long id, TransactionDTO dto) {
        log.info("Updating transaction ID={}", id);

        Transaction existing = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));

        // Cannot amend finalized transactions
        if ("SUCCESS".equalsIgnoreCase(existing.getStatus()) || "FAILED".equalsIgnoreCase(existing.getStatus())) {
            throw new InvalidTransactionException(
                    "Cannot amend a transaction with status: " + existing.getStatus() + ". Only PENDING transactions can be amended.");
        }

        // Validate new amount
        if (dto.getAmount() != null && dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transaction amount must be positive");
        }

        transactionMapper.updateEntityFromDTO(dto, existing);
        // Preserve original transaction date
        // existing.setTransactionDate stays unchanged

        Transaction updated = transactionRepository.save(existing);
        log.info("Transaction ID={} updated successfully", id);
        return transactionMapper.toDTO(updated);
    }

    /**
     * Cancel (delete) a transaction — only allowed for PENDING transactions.
     */
    public void deleteTransaction(Long id) {
        log.info("Cancelling transaction ID={}", id);

        Transaction existing = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));

        // Only PENDING transactions can be cancelled
        if ("SUCCESS".equalsIgnoreCase(existing.getStatus()) || "FAILED".equalsIgnoreCase(existing.getStatus())) {
            throw new InvalidTransactionException(
                    "Cannot cancel a transaction with status: " + existing.getStatus() + ". Only PENDING transactions can be cancelled.");
        }

        transactionRepository.deleteById(id);
        log.info("Transaction ID={} cancelled", id);
    }
}
