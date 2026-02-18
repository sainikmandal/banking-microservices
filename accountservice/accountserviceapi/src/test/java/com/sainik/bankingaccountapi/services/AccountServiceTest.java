package com.sainik.bankingaccountapi.services;

import com.sainik.bankingaccountapi.dtos.AccountDTO;
import com.sainik.bankingaccountapi.exceptions.AccountAlreadyExistsException;
import com.sainik.bankingaccountapi.exceptions.AccountNotFoundException;
import com.sainik.bankingaccountapi.mappers.AccountMapper;
import com.sainik.bankingaccountapi.models.Account;
import com.sainik.bankingaccountapi.repositories.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService Unit Tests")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService accountService;

    private Account account;
    private AccountDTO accountDTO;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setAccountNumber("ACC001");
        account.setCustomerId(10L);
        account.setType("Savings");
        account.setBalance(new BigDecimal("5000.00"));
        account.setCreatedAt(LocalDateTime.now());

        accountDTO = new AccountDTO();
        accountDTO.setAccountNumber("ACC001");
        accountDTO.setCustomerId(10L);
        accountDTO.setType("Savings");
        accountDTO.setBalance(new BigDecimal("5000.00"));
    }

    // ─── addAccount ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("addAccount: should create and return account when account number is unique")
    void addAccount_success() {
        when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.empty());
        when(accountMapper.dtotoentity(accountDTO)).thenReturn(account);
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        Account result = accountService.addAccount(accountDTO);

        assertThat(result).isNotNull();
        assertThat(result.getAccountNumber()).isEqualTo("ACC001");
        assertThat(result.getBalance()).isEqualByComparingTo("5000.00");
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("addAccount: should throw AccountAlreadyExistsException when account number is taken")
    void addAccount_duplicateAccountNumber_throwsException() {
        when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(account));

        assertThrows(AccountAlreadyExistsException.class,
                () -> accountService.addAccount(accountDTO));

        verify(accountRepository, never()).save(any());
    }

    // ─── getAllAccounts ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllAccounts: should return all accounts from repository")
    void getAllAccounts_returnsAll() {
        Account second = new Account();
        second.setId(2L);
        second.setAccountNumber("ACC002");

        when(accountRepository.findAll()).thenReturn(List.of(account, second));

        List<Account> result = accountService.getAllAccounts();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAccountNumber()).isEqualTo("ACC001");
        verify(accountRepository).findAll();
    }

    @Test
    @DisplayName("getAllAccounts: should return empty list when no accounts exist")
    void getAllAccounts_emptyRepository_returnsEmptyList() {
        when(accountRepository.findAll()).thenReturn(List.of());

        List<Account> result = accountService.getAllAccounts();

        assertThat(result).isEmpty();
    }

    // ─── getAccountById ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAccountById: should return account when ID exists")
    void getAccountById_found() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        Account result = accountService.getAccountById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAccountNumber()).isEqualTo("ACC001");
    }

    @Test
    @DisplayName("getAccountById: should throw AccountNotFoundException when ID does not exist")
    void getAccountById_notFound_throwsException() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        AccountNotFoundException ex = assertThrows(AccountNotFoundException.class,
                () -> accountService.getAccountById(99L));

        assertThat(ex.getMessage()).contains("99");
    }

    // ─── getAccountByNumber ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getAccountByNumber: should return account when number exists")
    void getAccountByNumber_found() {
        when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(account));

        Account result = accountService.getAccountByNumber("ACC001");

        assertThat(result.getAccountNumber()).isEqualTo("ACC001");
    }

    @Test
    @DisplayName("getAccountByNumber: should throw AccountNotFoundException when number not found")
    void getAccountByNumber_notFound_throwsException() {
        when(accountRepository.findByAccountNumber("INVALID")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.getAccountByNumber("INVALID"));
    }

    // ─── updateAccount ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateAccount: should update fields and save when account exists")
    void updateAccount_success() {
        AccountDTO updateDTO = new AccountDTO();
        updateDTO.setAccountNumber("ACC001-UPDATED");
        updateDTO.setCustomerId(10L);
        updateDTO.setType("Current");
        updateDTO.setBalance(new BigDecimal("9999.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.updateAccount(1L, updateDTO);

        assertThat(result.getAccountNumber()).isEqualTo("ACC001-UPDATED");
        assertThat(result.getType()).isEqualTo("Current");
        assertThat(result.getBalance()).isEqualByComparingTo("9999.00");
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("updateAccount: should throw AccountNotFoundException when account does not exist")
    void updateAccount_notFound_throwsException() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.updateAccount(99L, accountDTO));

        verify(accountRepository, never()).save(any());
    }

    // ─── deleteAccount ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteAccount: should delete account when ID exists")
    void deleteAccount_success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        accountService.deleteAccount(1L);

        verify(accountRepository).delete(account);
    }

    @Test
    @DisplayName("deleteAccount: should throw AccountNotFoundException when ID does not exist")
    void deleteAccount_notFound_throwsException() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.deleteAccount(99L));

        verify(accountRepository, never()).delete(any());
    }
}
