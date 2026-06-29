package com.barry.bank.domain.exception;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Raised when a debit would drive an account balance below zero.
 * Maps to HTTP 422 Unprocessable Entity.
 */
public class InsufficientBalanceException extends DomainException {

    public InsufficientBalanceException(UUID accountId, BigDecimal balance, BigDecimal amount) {
        super("Insufficient balance for a debit transaction on account " + accountId
                + ": balance=" + balance + ", requested=" + amount);
    }
}