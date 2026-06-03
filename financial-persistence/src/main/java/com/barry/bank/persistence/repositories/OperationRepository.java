package com.barry.bank.persistence.repositories;

import com.barry.bank.domain.entities.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OperationRepository extends JpaRepository<Operation, UUID> {

    List<Operation> findByAccount_AccountId(UUID accountId, Sort sort);

    List<Operation> findByAccount_AccountId(UUID accountId);

    Page<Operation> findByAccount_AccountId(UUID accountId, Pageable pageable);

    void deleteAllByAccount_AccountId(UUID accountId);

    List<Operation> findByAccount_AccountIdAndOperationDateBetweenOrderByOperationDateAsc(
            UUID accountId, LocalDateTime from, LocalDateTime to);

    List<Operation> findByAccount_AccountIdAndOperationDateAfter(
            UUID accountId, LocalDateTime after);
}
