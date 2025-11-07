package com.barry.bank.financial.tracking_ws.services.impl;

import com.barry.bank.financial.tracking_ws.entities.Customer;
import com.barry.bank.financial.tracking_ws.repositories.CustomerRepository;
import com.barry.bank.financial.tracking_ws.services.CustomerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    public Customer createCustomer(Customer customer) {
        if (customer == null) {
            log.warn("Attempted to create a null customer");
            throw new IllegalArgumentException("Customer must not be null");
        }

        final String email = customer.getEmail();
        if (StringUtils.isBlank(email)) {
            log.warn("Attempted to create customer with a null email. email: {}",  email);
            throw new IllegalArgumentException("Customer email must not be null");
        }
        if (customerRepository.existsByEmailIgnoreCase(email)) {
            log.warn("Attempted to create customer with an email that already exists. email: {}", email);
            throw new IllegalArgumentException("Customer with this email already exists");
        }

        Customer savedCustomer = customerRepository.save(customer);
        log.info("Customer saved successfully. customerId: {}, email: {}",
                savedCustomer.getCustomerId(), savedCustomer.getEmail());
        return savedCustomer;
    }

    @Override
    public List<Customer> getCustomersPaginated(int page, int size) {
        if (page < 0 || size <= 0) {
            log.warn("Page must be greater than or equal to zero, and  size must be greater than zero. Page: {}, size: {}", page, size);
            throw new IllegalArgumentException("Page must be greater than or equal to zero, and size must be greater than zero");
        }

        List<Customer> customers = customerRepository.findAll(PageRequest.of(page, size)).getContent();
        log.info("Successfully retrieved {} customers for page: {}, size: {}", customers.size(), page, size);
        return customers;
    }

    @Override
    public List<Customer> getCustomersWithoutPagination() {
        final List<Customer> customers = customerRepository.findAll();
        if (CollectionUtils.isEmpty(customers)) {
            log.warn("Attempted to retrieve customers from database, but none were found.");
            throw new IllegalStateException("No customers found in the database");
        }
        log.info("Successfully retrieved: {} customers from database ", customers.size());
        return customers;
    }

    @Override
    public Customer getCustomerById(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    log.warn("Attempt to retrieve non-existent customerId: {}", customerId);
                    return new IllegalArgumentException("Customer not found with ID:" + customerId);
                });
    }


    @Override
    public Customer partiallyUpdateCustomer(UUID customerId, Customer customer) {
        if (customer == null || customerId == null) {
            log.warn("Attempt to update customer with null value. customerId: {}, customer: {}", customerId, customer);
            throw new IllegalArgumentException("Customer ID and customer data must not be null");
        }

        Customer existingCustomer = getCustomerById(customerId);
        // update only non-null fields
        Optional.ofNullable(customer.getFirstName()).ifPresent(existingCustomer::setFirstName);
        Optional.ofNullable(customer.getLastName()).ifPresent(existingCustomer:: setLastName);
        Optional.ofNullable(customer.getGender()).ifPresent(existingCustomer:: setGender);
        Optional.ofNullable(customer.getEmail()).ifPresent(existingCustomer:: setEmail);

        Customer savedCustomer = customerRepository.save(existingCustomer);
        log.info("Partially updating customer with. customerId: {}", customerId);
        return savedCustomer;
    }

    @Override
    public void deleteCustomer(UUID customerId) {
        Customer customer = getCustomerById(customerId);
        customerRepository.delete(customer);
        log.info("Customer deleted successfully: customerId: {}",customerId);
    }
}
