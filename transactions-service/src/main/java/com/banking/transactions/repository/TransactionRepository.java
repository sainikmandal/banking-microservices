package com.banking.transactions.repository;

import com.banking.transactions.entity.Transaction;
import com.banking.transactions.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountId(Long accountId);

    List<Transaction> findByAccountIdAndStatus(Long accountId, TransactionStatus status);

    List<Transaction> findByAccountIdAndTransactionDateBetween(
            Long accountId, LocalDateTime start, LocalDateTime end);

    Optional<Transaction> findByReferenceNumber(String referenceNumber);
}
