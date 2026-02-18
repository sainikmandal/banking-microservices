package com.sainik.bankingaccountapi.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sainik.bankingaccountapi.dtos.AccountDTO;
import com.sainik.bankingaccountapi.exceptions.AccountAlreadyExistsException;
import com.sainik.bankingaccountapi.exceptions.AccountNotFoundException;
import com.sainik.bankingaccountapi.mappers.AccountMapper;
import com.sainik.bankingaccountapi.models.Account;
import com.sainik.bankingaccountapi.repositories.AccountRepository;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountMapper accountMapper;

    // Create
    public Account addAccount(AccountDTO accountDTO) {
        accountRepository.findByAccountNumber(accountDTO.getAccountNumber())
                .ifPresent(existing -> {
                    throw new AccountAlreadyExistsException(
                            "Account with number " + accountDTO.getAccountNumber() + " already exists");
                });

        Account account = accountMapper.dtotoentity(accountDTO);
        account.setCreatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }

    // Read All
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    // Read One by ID
    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + id));
    }

    // Read One by Account Number
    public Account getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with number: " + accountNumber));
    }

    // Update (full update)
    public Account updateAccount(Long id, AccountDTO accountDTO) {
        Account account = getAccountById(id);
        account.setAccountNumber(accountDTO.getAccountNumber());
        account.setCustomerId(accountDTO.getCustomerId());
        account.setType(accountDTO.getType());
        account.setBalance(accountDTO.getBalance());
        return accountRepository.save(account);
    }

    // Delete
    public void deleteAccount(Long id) {
        Account account = getAccountById(id);
        accountRepository.delete(account);
    }
}