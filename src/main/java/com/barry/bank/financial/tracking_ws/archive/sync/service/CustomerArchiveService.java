package com.barry.bank.financial.tracking_ws.archive.sync.service;

import com.barry.bank.financial.tracking_ws.entities.Customer;

public interface CustomerArchiveService {


    /**
     * Build the archive DTO for sending to the archiving service.
     *
     * @param customer entity Customer to archived
     */
    void archiveCustomer(Customer customer);
}
