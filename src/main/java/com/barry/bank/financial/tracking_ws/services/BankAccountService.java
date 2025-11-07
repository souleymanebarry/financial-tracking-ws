package com.barry.bank.financial.tracking_ws.services;

import com.barry.bank.financial.tracking_ws.entities.BankAccount;
import com.barry.bank.financial.tracking_ws.entities.CurrentAccount;
import com.barry.bank.financial.tracking_ws.entities.Customer;
import com.barry.bank.financial.tracking_ws.entities.Operation;
import com.barry.bank.financial.tracking_ws.entities.SavingAccount;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BankAccountService {

    /**
     *  Create a current account for an existing customer
     *
     * @param account The CurrentAccount to be created
     * @param customer the customer whom the account will be linked
     * @return the created CurrentAccount
     */
    CurrentAccount createCurrentAccount(CurrentAccount account, Customer customer);


    /**
     * Create saving account for an existing customer
     *
     * @param account the saving account to be created
     * @param customer: the customer whom the account will be linked
     * @return the created saving account
     */
    SavingAccount createSavingAccount(SavingAccount account, Customer customer);

    /**
     * Debits an amount from account and saves the operation (within a transaction)
     *
     * @param accountId The ID of account to be debited
     * @param amount The amount to be debited
     * @param description  a description of the operation
     */
    void debitAccount(UUID accountId, BigDecimal amount, String description);

    /**
     * Credits an amount to an account and saves the operation (within a transaction)
     *
     * @param accountId The ID of account to be credited
     * @param amount amount to be credited to the account
     * @param description description of the operation
     */
    void creditAccount(UUID accountId, BigDecimal amount, String description);

    /**
     * Transfers an amount between two different accounts and saves the operations (within transaction)
     *
     * @param sourceAccountId The ID of account to be debited
     * @param destinationAccountId The ID of account to be credited
     * @param amount amount of the transaction
     */
    void transferBetweenAccounts(UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount);

    /**
     * Retrieves a paginated list of bank accounts
     *
     * @param page the page number (zero-based)
     * @param size size the number of elements per page
     * @return a list of bank account for the requested page
     */
    List<BankAccount> getAccountsPaginated(int page, int size);

    /**
     * Retrieves a paginated list operations for accountId
     *
     * @param accountId the ID of the account whose operations are to be retrieved
     * @param page the page number
     * @param size size the number of elements per page
     * @return a page containing the operations for the requested account
     */
    Page<Operation> getAccountOperationsPage(UUID accountId, int page, int size);

    /**
     * Retrieves the transaction for a specific account
     *
     * @param accountId the ID of the account whose operations are to be retrieved
     * @return a list of Operations linked to the account
     */
    List<Operation> getAccountTransactionHistory(UUID accountId);

    /**
     * Retrieve account by ID
     *
     * @param accountId the specific accountID
     * @return the account retrieved
     */
    BankAccount getAccountById(UUID accountId);


    List<BankAccount> getAccountsWithoutPaginations();
}
