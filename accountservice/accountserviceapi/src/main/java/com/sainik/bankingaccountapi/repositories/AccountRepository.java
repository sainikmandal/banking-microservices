package com.sainik.bankingaccountapi.repositories;

import com.sainik.bankingaccountapi.models.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    // Custom finder method
    Optional<Account> findByAccountNumber(String accountNumber);
}