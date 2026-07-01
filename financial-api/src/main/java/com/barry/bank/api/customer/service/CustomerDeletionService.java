package com.barry.bank.api.customer.service;

import java.util.UUID;

public interface CustomerDeletionService {

    /**
     * Archives a customer (with all accounts and operations) then deletes it and all related data.
     * <p>
     * Archiving is performed <b>before</b> deletion so customer data is never lost. Because the
     * archive step is an external call that cannot participate in the database transaction, the
     * operation is designed to be safely retriable: the archive call is expected to be idempotent
     * (keyed by {@code customerId}), and the deletion runs in its own transaction so it stays atomic.
     *
     * @param customerId the unique identifier of the customer to archive and delete
     */
    void archiveAndDeleteCustomer(UUID customerId);
}