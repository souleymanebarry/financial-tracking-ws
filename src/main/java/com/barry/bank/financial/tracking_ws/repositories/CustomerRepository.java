package com.barry.bank.financial.tracking_ws.repositories;

import com.barry.bank.financial.tracking_ws.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    boolean existsByEmailIgnoreCase(String email);

}
