package com.barry.bank.api.customer.service.impl;

import com.barry.bank.api.archive.sync.service.CustomerArchiveService;
import com.barry.bank.api.customer.service.CustomerDeletionService;
import com.barry.bank.application.services.CustomerService;
import com.barry.bank.domain.model.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
public class CustomerDeletionServiceImpl implements CustomerDeletionService {

    private final CustomerService customerService;
    private final CustomerArchiveService customerArchiveService;

    @Override
    public void archiveAndDeleteCustomer(UUID customerId) {
        // 1. Load the full aggregate (accounts + operations) required for archiving.
        Customer customer = customerService.getCustomerWithAccountsAndOperations(customerId);

        // 2. Archive BEFORE deleting so data is never lost. The archive call must be idempotent
        //    so the whole operation can be safely retried if the deletion below fails.
        customerArchiveService.archiveCustomer(customer);
        log.info("Archiving completed for customer {}", customerId);

        // 3. Delete the customer and all related data. deleteCustomer is @Transactional, so the
        //    database removal is atomic on its own (no partial delete if a step fails).
        customerService.deleteCustomer(customerId);
        log.info("Customer {} archived and deleted successfully", customerId);
    }
}
