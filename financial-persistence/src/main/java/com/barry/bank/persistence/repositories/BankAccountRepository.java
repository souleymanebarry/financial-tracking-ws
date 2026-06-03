package com.barry.bank.persistence.repositories;


import com.barry.bank.domain.entities.BankAccount;
import com.barry.bank.domain.entities.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;


public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {

    boolean existsByRib(String rib);

    List<BankAccount> findByCustomer_CustomerId(UUID customerId);

    List<BankAccount> findByStatus(AccountStatus status);
}
