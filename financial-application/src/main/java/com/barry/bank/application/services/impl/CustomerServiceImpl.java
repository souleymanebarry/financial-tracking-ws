package com.barry.bank.application.services.impl;

import com.barry.bank.application.services.CustomerService;
import com.barry.bank.domain.entities.BankAccount;
import com.barry.bank.domain.entities.Customer;
import com.barry.bank.domain.entities.Operation;
import com.barry.bank.domain.exception.DuplicateResourceException;
import com.barry.bank.domain.exception.ResourceNotFoundException;
import com.barry.bank.persistence.repositories.BankAccountRepository;
import com.barry.bank.persistence.repositories.CustomerRepository;
import com.barry.bank.persistence.repositories.OperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final BankAccountRepository accountRepository;
    private final OperationRepository operationRepository;

    @Override
    @Transactional
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
            throw new DuplicateResourceException("Customer with this email already exists");
        }

        Customer savedCustomer = customerRepository.save(customer);
        log.info("Customer saved successfully. customerId: {}, email: {}",
                savedCustomer.getCustomerId(), savedCustomer.getEmail());
        return savedCustomer;
    }

    @Override
    public List<Customer> getCustomersPaginated(int page, int size) {
        List<Customer> customers = customerRepository.findAll(PageRequest.of(page, size)).getContent();
        log.info("Successfully retrieved {} customers for page: {}, size: {}", customers.size(), page, size);
        return customers;
    }

    @Override
    public List<Customer> getCustomersWithoutPagination() {
        final List<Customer> customers = customerRepository.findAll();
        log.info("Successfully retrieved: {} customers from database", customers.size());
        return customers;
    }

    @Override
    public Customer getCustomerById(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    log.warn("Attempt to retrieve non-existent customerId: {}", customerId);
                    return new ResourceNotFoundException("Customer not found with ID:" + customerId);
                });
    }


    @Override
    @Transactional
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
    public Customer getFullCustomerData(UUID customerId) {
        Customer customer = getCustomerById(customerId);
        List<BankAccount> accounts = accountRepository.findByCustomer_CustomerId(customerId);
        accounts.forEach(account -> {
            List<Operation> operations = operationRepository.findByAccount_AccountId(account.getAccountId());
            account.setOperations(operations);
        });
        customer.setBankAccounts(accounts);
        return customer;
    }

    @Transactional
    public void deleteCustomer(UUID customerId) {
        Customer customer = getCustomerById(customerId);

        List<BankAccount> accounts = getBankAccounts(customerId);

        // supprimer les opérations de chaque compte
        accounts.forEach(account -> {
            operationRepository.deleteAllByAccount_AccountId(account.getAccountId());
            accountRepository.delete(account);
        });
        customerRepository.delete(customer);
        log.info("Customer and all related data deleted successfully: customerId={}", customerId);
    }

    private List<BankAccount> getBankAccounts(UUID customerId) {
        return accountRepository.findByCustomer_CustomerId(customerId);
    }
}
