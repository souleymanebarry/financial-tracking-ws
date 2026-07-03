package com.barry.bank.persistence.repositories;


import com.barry.bank.domain.model.BankAccount;
import com.barry.bank.domain.enumerations.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;


public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {

    boolean existsByRib(String rib);

    List<BankAccount> findByCustomer_CustomerId(UUID customerId);

    Page<BankAccount> findByStatus(AccountStatus status, Pageable pageable);

    /**
     * Bulk-deletes, in a single query, all accounts belonging to the given customer.
     * Operations must be deleted first to satisfy the foreign-key constraint.
     */
    @Modifying
    @Query("DELETE FROM Account a WHERE a.customer.customerId = :customerId")
    void deleteByCustomerId(@Param("customerId") UUID customerId);
}
