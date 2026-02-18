package com.sainik.bankingtransaction.exceptions;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(String message) {
        super(message);
    }

    public TransactionNotFoundException(Long id) {
        super("Transaction not found with ID: " + id);
    }
}
