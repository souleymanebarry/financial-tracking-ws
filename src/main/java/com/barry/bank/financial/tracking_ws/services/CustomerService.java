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

    List<Customer> getCustomersPaginated(int page, int size);

    List<Customer> getCustomersWithoutPagination();

    Customer getCustomerById(UUID customerId);

    Customer partiallyUpdateCustomer(UUID customerId, Customer customer);

    void deleteCustomer(UUID customerId);

}
