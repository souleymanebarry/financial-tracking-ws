package com.barry.bank.financial.tracking_ws.services;

import com.barry.bank.financial.tracking_ws.entities.Customer;

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
    List<Customer> getCustomersPaginated(int page, int size);

    /**
     * Retrieve all customers without pagination
     *
     * @return a list of {@link Customer}  objects
     */
    List<Customer> getCustomersWithoutPagination();

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
    Customer getFullCustomerData(UUID customerId);

    /**
     * Delete all operations and all accounts of a customer before deleting the customer
     *
     * @param customerId the unique identifier of the customer to delete
     */
    void deleteCustomer(UUID customerId);

}
