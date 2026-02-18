package com.sainik.bankingtransaction.services;

import com.sainik.bankingtransaction.dtos.TransactionDTO;
import com.sainik.bankingtransaction.exceptions.InvalidTransactionException;
import com.sainik.bankingtransaction.exceptions.TransactionNotFoundException;
import com.sainik.bankingtransaction.mappers.TransactionMapper;
import com.sainik.bankingtransaction.models.Transaction;
import com.sainik.bankingtransaction.repositories.TransactionRepository;
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
@DisplayName("TransactionService Unit Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

    private Transaction pendingTransaction;
    private Transaction successTransaction;
    private TransactionDTO transactionDTO;

    @BeforeEach
    void setUp() {
        pendingTransaction = new Transaction();
        pendingTransaction.setId(1L);
        pendingTransaction.setAccountId(10L);
        pendingTransaction.setType("Deposit");
        pendingTransaction.setAmount(new BigDecimal("500.00"));
        pendingTransaction.setStatus("PENDING");
        pendingTransaction.setTransactionDate(LocalDateTime.now());

        successTransaction = new Transaction();
        successTransaction.setId(2L);
        successTransaction.setAccountId(10L);
        successTransaction.setType("Withdrawal");
        successTransaction.setAmount(new BigDecimal("100.00"));
        successTransaction.setStatus("SUCCESS");
        successTransaction.setTransactionDate(LocalDateTime.now());

        transactionDTO = new TransactionDTO();
        transactionDTO.setAccountId(10L);
        transactionDTO.setType("Deposit");
        transactionDTO.setAmount(new BigDecimal("500.00"));
        transactionDTO.setStatus("PENDING");
    }

    // ─── createTransaction ────────────────────────────────────────────────────────

    @Test
    @DisplayName("createTransaction: should create and return transaction with positive amount")
    void createTransaction_success() {
        when(transactionMapper.toEntity(transactionDTO)).thenReturn(pendingTransaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(pendingTransaction);
        when(transactionMapper.toDTO(pendingTransaction)).thenReturn(transactionDTO);

        TransactionDTO result = transactionService.createTransaction(transactionDTO);

        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo("500.00");
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("createTransaction: should throw InvalidTransactionException when amount is zero")
    void createTransaction_zeroAmount_throwsException() {
        transactionDTO.setAmount(BigDecimal.ZERO);

        assertThrows(InvalidTransactionException.class,
                () -> transactionService.createTransaction(transactionDTO));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTransaction: should throw InvalidTransactionException when amount is negative")
    void createTransaction_negativeAmount_throwsException() {
        transactionDTO.setAmount(new BigDecimal("-100.00"));

        assertThrows(InvalidTransactionException.class,
                () -> transactionService.createTransaction(transactionDTO));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTransaction: should throw InvalidTransactionException when amount is null")
    void createTransaction_nullAmount_throwsException() {
        transactionDTO.setAmount(null);

        assertThrows(InvalidTransactionException.class,
                () -> transactionService.createTransaction(transactionDTO));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("createTransaction: should set transactionDate automatically on new transaction")
    void createTransaction_setsTransactionDate() {
        Transaction unsaved = new Transaction();
        unsaved.setAccountId(10L);
        unsaved.setType("Deposit");
        unsaved.setAmount(new BigDecimal("250.00"));
        unsaved.setStatus("PENDING");
        // transactionDate intentionally null — service should set it

        when(transactionMapper.toEntity(transactionDTO)).thenReturn(unsaved);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionMapper.toDTO(any(Transaction.class))).thenReturn(transactionDTO);

        transactionService.createTransaction(transactionDTO);

        assertThat(unsaved.getTransactionDate()).isNotNull();
    }

    // ─── getAllTransactions ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllTransactions: should return mapped DTOs for all transactions")
    void getAllTransactions_returnsAll() {
        when(transactionRepository.findAll()).thenReturn(List.of(pendingTransaction, successTransaction));
        when(transactionMapper.toDTO(pendingTransaction)).thenReturn(transactionDTO);
        when(transactionMapper.toDTO(successTransaction)).thenReturn(new TransactionDTO());

        List<TransactionDTO> result = transactionService.getAllTransactions();

        assertThat(result).hasSize(2);
        verify(transactionRepository).findAll();
    }

    @Test
    @DisplayName("getAllTransactions: should return empty list when no transactions exist")
    void getAllTransactions_emptyRepository_returnsEmptyList() {
        when(transactionRepository.findAll()).thenReturn(List.of());

        List<TransactionDTO> result = transactionService.getAllTransactions();

        assertThat(result).isEmpty();
    }

    // ─── getTransactionById ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getTransactionById: should return transaction DTO when ID exists")
    void getTransactionById_found() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(pendingTransaction));
        when(transactionMapper.toDTO(pendingTransaction)).thenReturn(transactionDTO);

        TransactionDTO result = transactionService.getTransactionById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getAccountId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getTransactionById: should throw TransactionNotFoundException when ID does not exist")
    void getTransactionById_notFound_throwsException() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        TransactionNotFoundException ex = assertThrows(TransactionNotFoundException.class,
                () -> transactionService.getTransactionById(99L));

        assertThat(ex.getMessage()).contains("99");
    }

    // ─── getTransactionsByAccountId ───────────────────────────────────────────────

    @Test
    @DisplayName("getTransactionsByAccountId: should return all transactions for a given account")
    void getTransactionsByAccountId_found() {
        when(transactionRepository.findByAccountId(10L)).thenReturn(List.of(pendingTransaction, successTransaction));
        when(transactionMapper.toDTO(any(Transaction.class))).thenReturn(transactionDTO);

        List<TransactionDTO> result = transactionService.getTransactionsByAccountId(10L);

        assertThat(result).hasSize(2);
        verify(transactionRepository).findByAccountId(10L);
    }

    @Test
    @DisplayName("getTransactionsByAccountId: should return empty list when account has no transactions")
    void getTransactionsByAccountId_noTransactions_returnsEmptyList() {
        when(transactionRepository.findByAccountId(99L)).thenReturn(List.of());

        List<TransactionDTO> result = transactionService.getTransactionsByAccountId(99L);

        assertThat(result).isEmpty();
    }

    // ─── updateTransaction (business rule: PENDING only) ─────────────────────────

    @Test
    @DisplayName("updateTransaction: should amend a PENDING transaction successfully")
    void updateTransaction_pendingTransaction_success() {
        TransactionDTO updateDTO = new TransactionDTO();
        updateDTO.setAccountId(10L);
        updateDTO.setType("Withdrawal");
        updateDTO.setAmount(new BigDecimal("300.00"));
        updateDTO.setStatus("PENDING");

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(pendingTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(pendingTransaction);
        when(transactionMapper.toDTO(pendingTransaction)).thenReturn(updateDTO);

        TransactionDTO result = transactionService.updateTransaction(1L, updateDTO);

        assertThat(result).isNotNull();
        verify(transactionMapper).updateEntityFromDTO(eq(updateDTO), eq(pendingTransaction));
        verify(transactionRepository).save(pendingTransaction);
    }

    @Test
    @DisplayName("updateTransaction: should throw InvalidTransactionException when transaction is SUCCESS")
    void updateTransaction_successStatus_throwsException() {
        TransactionDTO updateDTO = new TransactionDTO();
        updateDTO.setAmount(new BigDecimal("999.00"));
        updateDTO.setStatus("PENDING");

        when(transactionRepository.findById(2L)).thenReturn(Optional.of(successTransaction));

        InvalidTransactionException ex = assertThrows(InvalidTransactionException.class,
                () -> transactionService.updateTransaction(2L, updateDTO));

        assertThat(ex.getMessage()).contains("SUCCESS");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateTransaction: should throw InvalidTransactionException when transaction is FAILED")
    void updateTransaction_failedStatus_throwsException() {
        Transaction failedTransaction = new Transaction();
        failedTransaction.setId(3L);
        failedTransaction.setStatus("FAILED");
        failedTransaction.setAmount(new BigDecimal("50.00"));

        TransactionDTO updateDTO = new TransactionDTO();
        updateDTO.setAmount(new BigDecimal("999.00"));

        when(transactionRepository.findById(3L)).thenReturn(Optional.of(failedTransaction));

        assertThrows(InvalidTransactionException.class,
                () -> transactionService.updateTransaction(3L, updateDTO));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateTransaction: should throw InvalidTransactionException when new amount is negative")
    void updateTransaction_negativeAmount_throwsException() {
        TransactionDTO updateDTO = new TransactionDTO();
        updateDTO.setAmount(new BigDecimal("-50.00"));

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(pendingTransaction));

        assertThrows(InvalidTransactionException.class,
                () -> transactionService.updateTransaction(1L, updateDTO));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateTransaction: should throw TransactionNotFoundException when ID does not exist")
    void updateTransaction_notFound_throwsException() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class,
                () -> transactionService.updateTransaction(99L, transactionDTO));
    }

    // ─── deleteTransaction (business rule: PENDING only) ─────────────────────────

    @Test
    @DisplayName("deleteTransaction: should cancel a PENDING transaction successfully")
    void deleteTransaction_pendingTransaction_success() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(pendingTransaction));

        transactionService.deleteTransaction(1L);

        verify(transactionRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteTransaction: should throw InvalidTransactionException when transaction is SUCCESS")
    void deleteTransaction_successStatus_throwsException() {
        when(transactionRepository.findById(2L)).thenReturn(Optional.of(successTransaction));

        InvalidTransactionException ex = assertThrows(InvalidTransactionException.class,
                () -> transactionService.deleteTransaction(2L));

        assertThat(ex.getMessage()).contains("SUCCESS");
        verify(transactionRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteTransaction: should throw InvalidTransactionException when transaction is FAILED")
    void deleteTransaction_failedStatus_throwsException() {
        Transaction failedTransaction = new Transaction();
        failedTransaction.setId(3L);
        failedTransaction.setStatus("FAILED");
        failedTransaction.setAmount(new BigDecimal("50.00"));

        when(transactionRepository.findById(3L)).thenReturn(Optional.of(failedTransaction));

        assertThrows(InvalidTransactionException.class,
                () -> transactionService.deleteTransaction(3L));

        verify(transactionRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteTransaction: should throw TransactionNotFoundException when ID does not exist")
    void deleteTransaction_notFound_throwsException() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class,
                () -> transactionService.deleteTransaction(99L));

        verify(transactionRepository, never()).deleteById(any());
    }
}
