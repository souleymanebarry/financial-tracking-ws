package com.barry.bank.application.services;

import java.math.BigDecimal;
import java.util.UUID;

public interface OperationService {

    /**
     * Debits an amount from account and saves the operation (within a transaction)
     *
     * @param accountId   The ID of account to be debited
     * @param amount      The amount to be debited
     * @param description a description of the operation
     */
    void debitAccount(UUID accountId, BigDecimal amount, String description);

    /**
     * Credits an amount to an account and saves the operation (within a transaction)
     *
     * @param accountId   The ID of account to be credited
     * @param amount      amount to be credited to the account
     * @param description description of the operation
     */
    void creditAccount(UUID accountId, BigDecimal amount, String description);
}