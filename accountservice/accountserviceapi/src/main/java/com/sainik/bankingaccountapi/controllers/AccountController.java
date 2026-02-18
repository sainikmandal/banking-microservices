package com.sainik.bankingaccountapi.controllers;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.sainik.bankingaccountapi.dtos.AccountDTO;
import com.sainik.bankingaccountapi.dtos.GenericResponse;
import com.sainik.bankingaccountapi.mappers.AccountMapper;
import com.sainik.bankingaccountapi.models.Account;
import com.sainik.bankingaccountapi.services.AccountService;

@RestController
@RequestMapping("/accounts")
@CrossOrigin(origins = "*")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountMapper accountMapper;

    // POST - Create a new account
    @PreAuthorize("hasAnyAuthority('SCOPE_developer')")
    @PostMapping("/v1.0")
    public ResponseEntity<GenericResponse> addAccount(@Valid @RequestBody AccountDTO accountDTO) {
        Account savedAccount = accountService.addAccount(accountDTO);
        AccountDTO savedAccountDTO = accountMapper.entitytodto(savedAccount);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new GenericResponse(savedAccountDTO));
    }

    // GET - Retrieve all accounts
    @GetMapping("/v1.0")
    public ResponseEntity<GenericResponse> getAllAccounts() {
        List<Account> accounts = accountService.getAllAccounts();
        List<AccountDTO> accountDTOs = accountMapper.entitytolistdto(accounts);
        return ResponseEntity.ok(new GenericResponse(accountDTOs));
    }

    // GET - Retrieve account by ID
    @GetMapping("/v1.0/{id}")
    public ResponseEntity<GenericResponse> getAccountById(@PathVariable("id") Long id) {
        Account account = accountService.getAccountById(id);
        AccountDTO accountDTO = accountMapper.entitytodto(account);
        return ResponseEntity.ok(new GenericResponse(accountDTO));
    }

    // GET - Retrieve account by account number
    @GetMapping("/v1.0/number/{accountNumber}")
    public ResponseEntity<GenericResponse> getAccountByNumber(@PathVariable("accountNumber") String accountNumber) {
        Account account = accountService.getAccountByNumber(accountNumber);
        AccountDTO accountDTO = accountMapper.entitytodto(account);
        return ResponseEntity.ok(new GenericResponse(accountDTO));
    }

    // PUT - Update account information
    @PreAuthorize("hasAnyAuthority('SCOPE_developer')")
    @PutMapping("/v1.0/{id}")
    public ResponseEntity<GenericResponse> updateAccount(@PathVariable("id") Long id,
                                                         @Valid @RequestBody AccountDTO accountDTO) {
        Account updatedAccount = accountService.updateAccount(id, accountDTO);
        AccountDTO updatedDTO = accountMapper.entitytodto(updatedAccount);
        return ResponseEntity.ok(new GenericResponse(updatedDTO));
    }

    // DELETE - Close/remove an account
    @PreAuthorize("hasAnyAuthority('SCOPE_developer')")
    @DeleteMapping("/v1.0/{id}")
    public ResponseEntity<GenericResponse> deleteAccount(@PathVariable("id") Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.ok(new GenericResponse("Account deleted successfully"));
    }
}