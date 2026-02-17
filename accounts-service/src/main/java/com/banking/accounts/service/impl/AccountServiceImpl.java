package com.banking.accounts.service.impl;

import com.banking.accounts.dto.AccountRequestDto;
import com.banking.accounts.dto.AccountResponseDto;
import com.banking.accounts.dto.BalanceUpdateDto;
import com.banking.accounts.entity.Account;
import com.banking.accounts.entity.AccountStatus;
import com.banking.accounts.mapper.AccountMapper;
import com.banking.accounts.repository.AccountRepository;
import com.banking.accounts.service.AccountService;
import com.banking.common.exception.BusinessException;
import com.banking.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Override
    @Transactional
    public AccountResponseDto createAccount(AccountRequestDto dto) {
        String accountNumber = generateAccountNumber();

        Account account = accountMapper.toEntity(dto);
        account.setAccountNumber(accountNumber);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);

        Account saved = accountRepository.save(account);
        log.info("Account created: {}", saved.getAccountNumber());

        return accountMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponseDto getAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id));
        return accountMapper.toDto(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponseDto getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", accountNumber));
        return accountMapper.toDto(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponseDto> getAccountsByCustomer(Long customerId) {
        return accountRepository.findByCustomerId(customerId).stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AccountResponseDto updateAccount(Long id, AccountRequestDto dto) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id));

        account.setType(dto.getType());
        Account updated = accountRepository.save(account);
        log.info("Account updated: {}", updated.getAccountNumber());

        return accountMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deleteAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id));

        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Cannot close account with positive balance");
        }

        account.setStatus(AccountStatus.CLOSED);
        accountRepository.save(account);
        log.info("Account closed: {}", account.getAccountNumber());
    }

    @Override
    @Transactional
    public AccountResponseDto updateBalance(Long id, BalanceUpdateDto dto) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Account is not active");
        }

        BigDecimal newBalance;
        if (dto.getOperationType() == BalanceUpdateDto.OperationType.DEPOSIT) {
            newBalance = account.getBalance().add(dto.getAmount());
        } else {
            if (account.getBalance().compareTo(dto.getAmount()) < 0) {
                throw new BusinessException("Insufficient balance");
            }
            newBalance = account.getBalance().subtract(dto.getAmount());
        }

        account.setBalance(newBalance);
        Account updated = accountRepository.save(account);
        log.info("Balance updated for account {}: {}", account.getAccountNumber(), newBalance);

        return accountMapper.toDto(updated);
    }

    private String generateAccountNumber() {
        String accountNumber;
        do {
            accountNumber = "NL" +
                    String.format("%02d", ThreadLocalRandom.current().nextInt(100)) +
                    "BANK" +
                    String.format("%010d", ThreadLocalRandom.current().nextLong(10_000_000_000L));
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }
}
