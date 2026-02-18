package com.sainik.bankingtransaction.repositories;

import com.sainik.bankingtransaction.models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountId(Long accountId);

    List<Transaction> findByStatus(String status);

    List<Transaction> findByType(String type);
}
