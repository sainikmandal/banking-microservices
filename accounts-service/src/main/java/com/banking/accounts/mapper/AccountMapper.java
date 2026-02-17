package com.banking.accounts.mapper;

import com.banking.accounts.dto.AccountRequestDto;
import com.banking.accounts.dto.AccountResponseDto;
import com.banking.accounts.entity.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public Account toEntity(AccountRequestDto dto) {
        return Account.builder()
                .customerId(dto.getCustomerId())
                .type(dto.getType())
                .build();
    }

    public AccountResponseDto toDto(Account account) {
        return AccountResponseDto.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .type(account.getType())
                .balance(account.getBalance())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
