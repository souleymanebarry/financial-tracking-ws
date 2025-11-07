package com.barry.bank.financial.tracking_ws.repositories;

import com.barry.bank.financial.tracking_ws.entities.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {

    boolean existsByRib(String rib);

}
