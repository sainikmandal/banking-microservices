package com.banking.accounts.service;

import com.banking.accounts.dto.AccountRequestDto;
import com.banking.accounts.dto.AccountResponseDto;
import com.banking.accounts.dto.BalanceUpdateDto;

import java.util.List;

public interface AccountService {
    AccountResponseDto createAccount(AccountRequestDto dto);
    AccountResponseDto getAccount(Long id);
    AccountResponseDto getAccountByNumber(String accountNumber);
    List<AccountResponseDto> getAccountsByCustomer(Long customerId);
    AccountResponseDto updateAccount(Long id, AccountRequestDto dto);
    void deleteAccount(Long id);
    AccountResponseDto updateBalance(Long id, BalanceUpdateDto dto);
}
