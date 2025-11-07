package com.barry.bank.financial.tracking_ws.repositories;

import com.barry.bank.financial.tracking_ws.entities.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OperationRepository extends JpaRepository<Operation, UUID> {

    List<Operation> findByAccount_AccountId(UUID accountId, Sort sort);

    Page<Operation> findByAccount_AccountId(UUID accountId, Pageable pageable);


}
