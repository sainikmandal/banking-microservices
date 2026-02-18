package com.sainik.bankingaccountapi.mappers;

import java.util.List;

import org.mapstruct.Mapper;

import com.sainik.bankingaccountapi.dtos.AccountDTO;
import com.sainik.bankingaccountapi.models.Account;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    // dto to entity
    Account dtotoentity(AccountDTO accountDTO);

    // entity to dto
    AccountDTO entitytodto(Account account);

    // list of entity to list of dto
    List<AccountDTO> entitytolistdto(List<Account> accounts);

}