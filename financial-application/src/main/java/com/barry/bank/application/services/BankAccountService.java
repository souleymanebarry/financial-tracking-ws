package com.barry.bank.application.services;


import com.barry.bank.domain.entities.BankAccount;
import com.barry.bank.domain.entities.CurrentAccount;
import com.barry.bank.domain.entities.Customer;
import com.barry.bank.domain.entities.Operation;
import com.barry.bank.domain.entities.SavingAccount;
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
    List<BankAccount> getAccounts(int page, int size);

    /**
     *Retrieve all bank accounts without pagination
     *
     * @return a list of {@link BankAccount} objects
     */
    List<BankAccount> getAllAccounts();

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
    List<Operation> getAccountOperations(UUID accountId);

    /**
     * Retrieve account by ID
     *
     * @param accountId the specific accountID
     * @return the account retrieved
     */
    BankAccount getAccountById(UUID accountId);
}
