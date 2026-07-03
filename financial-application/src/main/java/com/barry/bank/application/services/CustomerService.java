package com.barry.bank.application.services;

import com.barry.bank.domain.model.Customer;

import java.util.List;
import java.util.UUID;

public interface CustomerService {

    /**
     * Create a new customer if he email is unique
     *
     * @param customer customer to create
     * @return the customer created
     */
    Customer createCustomer(Customer customer);

    /**
     *  Retrieve a paginated list of customers
     *
     * @param page the page number (zero-based)
     * @param size the size number of customer per page
     * @return a list of {@link Customer} objects for the specified page
     */
    List<Customer> getCustomers(int page, int size);

    /**
     * Retrieve all customers without pagination
     *
     * @return a list of {@link Customer}  objects
     */
    List<Customer> getAllCustomers();

    /**
     * Retrieves a customer by customerId
     *
     * @param customerId the ID used to retrieve customer
     * @return the {@link Customer} associated with the given ID
     */
    Customer getCustomerById(UUID customerId);

    /**
     * Partially update a customer with the given ID
     *
     * @param customerId the customer ID to update
     * @param customer the {@link Customer} object containing the fields to update
     * @return the updated {@link Customer} object
     */
    Customer partiallyUpdateCustomer(UUID customerId, Customer customer);

    /**
     * Retrieves customer along with all associated accounts and operations
     *
     * @param customerId the ID used to retrieve customer
     * @return the {@link Customer} including all  related accounts and operations
     */
    Customer getCustomerWithAccountsAndOperations(UUID customerId);

    /**
     * Deletes a customer and all related data. The removal of the customer's accounts and
     * their operations is delegated to {@link BankAccountService#deleteAccountsByCustomer(UUID)},
     * which owns the {@code BankAccount} aggregate.
     *
     * @param customerId the unique identifier of the customer to delete
     */
    void deleteCustomer(UUID customerId);

}
