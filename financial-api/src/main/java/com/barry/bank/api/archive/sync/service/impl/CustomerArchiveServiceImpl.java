package com.barry.bank.api.archive.sync.service.impl;


import com.barry.bank.api.archive.sync.client.ArchiveCustomer;
import com.barry.bank.api.archive.sync.dtos.AccountArchiveDTO;
import com.barry.bank.api.archive.sync.dtos.CustomerArchiveDTO;
import com.barry.bank.api.archive.sync.dtos.OperationArchiveDTO;
import com.barry.bank.api.archive.sync.service.CustomerArchiveService;
import com.barry.bank.domain.entities.BankAccount;
import com.barry.bank.domain.entities.CurrentAccount;
import com.barry.bank.domain.entities.Customer;
import com.barry.bank.domain.entities.Operation;
import com.barry.bank.domain.entities.SavingAccount;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class CustomerArchiveServiceImpl implements CustomerArchiveService {

    private final ArchiveCustomer archiveCustomer;

    public void archiveCustomer(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null");
        }

        CustomerArchiveDTO archiveDTO = buildCustomerArchiveDTO(customer);
        log.info("Sending archive data for customerId={}", customer.getCustomerId());
        archiveCustomer.archiveCustomerData(archiveDTO);
    }

    private CustomerArchiveDTO buildCustomerArchiveDTO(Customer customer) {
        List<AccountArchiveDTO> accounts = customer.getBankAccounts().stream()
                .map(this::buildAccountArchiveDTO)
                .toList();

        return CustomerArchiveDTO.builder()
                .customerId(customer.getCustomerId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .gender(customer.getGender().name())
                .accounts(accounts)
                .build();
    }

    /**
     * Build the AccountArchiveDTO from BankAccount entity
     */
    private AccountArchiveDTO buildAccountArchiveDTO(BankAccount account) {
        List<OperationArchiveDTO> operations = account.getOperations().stream()
                .map(this::buildOperationArchiveDTO)
                .toList();

        AccountArchiveDTO dto = AccountArchiveDTO.builder()
                .accountId(account.getAccountId())
                .balance(account.getBalance())
                .createdAt(account.getCreatedAt())
                .rib(account.getRib())
                .status(account.getStatus().name())
                .accountType(account instanceof CurrentAccount ? "CURRENT ACCOUNT" : "SAVING ACCOUNT")
                .operations(operations)
                .build();

        // Add the specific properties depending on the account type
        if (account instanceof CurrentAccount current) {
            dto.setOverDraft(current.getOverDraft());
        } else if (account instanceof SavingAccount saving) {
            dto.setInterestRate(saving.getInterestRate());
        }
        return dto;
    }

    /**
     * Build the OperationArchiveDTO from Operation entity
     */
    private OperationArchiveDTO buildOperationArchiveDTO(Operation operation) {
        return OperationArchiveDTO.builder()
                .operationId(operation.getOperationId())
                .operationNumber(operation.getOperationNumber())
                .operationAmount(operation.getOperationAmount())
                .operationDate(operation.getOperationDate())
                .operationType(operation.getOperationType().name())
                .description(operation.getDescription())
                .build();
    }

}
