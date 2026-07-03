package com.barry.bank.application.services.impl;

import com.barry.bank.application.services.BankAccountService;
import com.barry.bank.application.services.CustomerService;
import com.barry.bank.domain.model.BankAccount;
import com.barry.bank.domain.model.Customer;
import com.barry.bank.domain.model.Operation;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final BankAccountRepository accountRepository;
    private final OperationRepository operationRepository;
    private final BankAccountService accountService;

    @Override
    @Transactional
    public Customer createCustomer(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer must not be null");
        }

        final String email = customer.getEmail();
        if (StringUtils.isBlank(email)) {
            throw new IllegalArgumentException("Customer email must not be null");
        }
        if (customerRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Customer with this email already exists");
        }

        Customer savedCustomer = customerRepository.save(customer);
        log.info("Customer created: customerId={}", savedCustomer.getCustomerId());
        return savedCustomer;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> getCustomers(int page, int size) {
        List<Customer> customers = customerRepository.findAll(PageRequest.of(page, size)).getContent();
        log.debug("Retrieved {} customers for page: {}, size: {}", customers.size(), page, size);
        return customers;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        final List<Customer> customers = customerRepository.findAll();
        log.debug("Retrieved {} customers from database", customers.size());
        return customers;
    }

    @Override
    @Transactional(readOnly = true)
    public Customer getCustomerById(UUID customerId) {
        return findCustomerOrThrow(customerId);
    }


    @Override
    @Transactional
    public Customer partiallyUpdateCustomer(UUID customerId, Customer customer) {
        if (customer == null || customerId == null) {
            throw new IllegalArgumentException("Customer ID and customer data must not be null");
        }

        Customer existingCustomer = findCustomerOrThrow(customerId);
        // update only non-null fields
        Optional.ofNullable(customer.getFirstName()).ifPresent(existingCustomer::setFirstName);
        Optional.ofNullable(customer.getLastName()).ifPresent(existingCustomer:: setLastName);
        Optional.ofNullable(customer.getGender()).ifPresent(existingCustomer:: setGender);
        Optional.ofNullable(customer.getEmail()).ifPresent(existingCustomer:: setEmail);

        Customer savedCustomer = customerRepository.save(existingCustomer);
        log.info("Customer partially updated: customerId={}", customerId);
        return savedCustomer;
    }

    @Override
    @Transactional(readOnly = true)
    public Customer getCustomerWithAccountsAndOperations(UUID customerId) {
        Customer customer = findCustomerOrThrow(customerId);
        List<BankAccount> accounts = accountRepository.findByCustomer_CustomerId(customerId);

        if(!accounts.isEmpty()) {
            final List<UUID> accountIds = accounts.stream().map(BankAccount::getAccountId).toList();
            Map<UUID, List<Operation>> operationsByAccount =
                    operationRepository.findByAccount_AccountIdIn(accountIds).stream()
                    .collect(Collectors.groupingBy(operation -> operation.getAccount().getAccountId()));

            accounts.forEach(account -> account.setOperations(operationsByAccount.getOrDefault(account.getAccountId(), List.of())));
        }
        customer.setBankAccounts(accounts);
        return customer;
    }

    @Override
    @Transactional
    public void deleteCustomer(UUID customerId) {
        Customer customer = findCustomerOrThrow(customerId);
        // Deletion of accounts and their operations is owned by the BankAccount aggregate
        accountService.deleteAccountsByCustomer(customerId);
        customerRepository.delete(customer);
        log.info("Customer and all related data deleted successfully: customerId={}", customerId);
    }

    private Customer findCustomerOrThrow(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID:" + customerId));
    }
}
