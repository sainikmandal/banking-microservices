package com.sainik.bankingaccountapi.controllers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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

    @PreAuthorize("hasAnyAuthority('SCOPE_developer')")
    @PostMapping("/v1.0")
    public ResponseEntity<GenericResponse<AccountDTO>> addAccount(@Valid @RequestBody AccountDTO accountDTO) {
        Account savedAccount = accountService.addAccount(accountDTO);
        AccountDTO savedAccountDTO = accountMapper.entitytodto(savedAccount);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GenericResponse.success("Account created successfully", savedAccountDTO));
    }

    @GetMapping("/v1.0")
    public ResponseEntity<GenericResponse<List<AccountDTO>>> getAllAccounts() {
        List<Account> accounts = accountService.getAllAccounts();
        List<AccountDTO> accountDTOs = accountMapper.entitytolistdto(accounts);
        return ResponseEntity.ok(GenericResponse.success("Accounts retrieved successfully", accountDTOs));
    }

    @GetMapping("/v1.0/{id}")
    public ResponseEntity<GenericResponse<AccountDTO>> getAccountById(@PathVariable("id") Long id) {
        Account account = accountService.getAccountById(id);
        AccountDTO accountDTO = accountMapper.entitytodto(account);
        return ResponseEntity.ok(GenericResponse.success("Account retrieved successfully", accountDTO));
    }

    @GetMapping("/v1.0/number/{accountNumber}")
    public ResponseEntity<GenericResponse<AccountDTO>> getAccountByNumber(@PathVariable("accountNumber") String accountNumber) {
        Account account = accountService.getAccountByNumber(accountNumber);
        AccountDTO accountDTO = accountMapper.entitytodto(account);
        return ResponseEntity.ok(GenericResponse.success("Account retrieved successfully", accountDTO));
    }

    @PreAuthorize("hasAnyAuthority('SCOPE_developer')")
    @PutMapping("/v1.0/{id}")
    public ResponseEntity<GenericResponse<AccountDTO>> updateAccount(@PathVariable("id") Long id,
                                                                     @Valid @RequestBody AccountDTO accountDTO) {
        Account updatedAccount = accountService.updateAccount(id, accountDTO);
        AccountDTO updatedDTO = accountMapper.entitytodto(updatedAccount);
        return ResponseEntity.ok(GenericResponse.success("Account updated successfully", updatedDTO));
    }

    @PreAuthorize("hasAnyAuthority('SCOPE_developer')")
    @DeleteMapping("/v1.0/{id}")
    public ResponseEntity<GenericResponse<Object>> deleteAccount(@PathVariable("id") Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.ok(GenericResponse.success("Account deleted successfully", null));
    }

    /**
     * Internal probe — permit-all (no JWT required).
     * Called by other microservices (e.g. transaction-service) via their
     * {@code AccountServiceClient} to verify an account exists before
     * recording a transaction.
     */
    @GetMapping("/exists/{id}")
    public ResponseEntity<GenericResponse<Boolean>> accountExists(@PathVariable("id") Long id) {
        boolean exists = accountService.accountExists(id);
        return ResponseEntity.ok(GenericResponse.success("Account existence checked", exists));
    }

    /**
     * Internal balance update — called by transaction-service after a successful transaction.
     * Accepts a JSON body: { "delta": <signed BigDecimal> }
     *   positive delta = credit (Deposit)
     *   negative delta = debit  (Withdrawal / Transfer)
     */
    @PreAuthorize("hasAnyAuthority('SCOPE_developer')")
    @PatchMapping("/v1.0/{id}/balance")
    public ResponseEntity<GenericResponse<AccountDTO>> updateBalance(
            @PathVariable("id") Long id,
            @RequestBody Map<String, BigDecimal> body) {
        BigDecimal delta = body.get("delta");
        if (delta == null) {
            return ResponseEntity.badRequest()
                    .body(GenericResponse.error("Request body must contain 'delta' field"));
        }
        Account updated = accountService.updateBalance(id, delta);
        AccountDTO dto = accountMapper.entitytodto(updated);
        return ResponseEntity.ok(GenericResponse.success("Balance updated successfully", dto));
    }
}
