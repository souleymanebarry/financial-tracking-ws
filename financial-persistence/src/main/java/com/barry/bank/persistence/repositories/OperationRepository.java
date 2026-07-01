package com.barry.bank.persistence.repositories;

import com.barry.bank.domain.entities.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OperationRepository extends JpaRepository<Operation, UUID> {

    List<Operation> findByAccount_AccountId(UUID accountId, Sort sort);

    List<Operation> findByAccount_AccountId(UUID accountId);

    Page<Operation> findByAccount_AccountId(UUID accountId, Pageable pageable);

    void deleteAllByAccount_AccountId(UUID accountId);

    List<Operation> findByAccount_AccountIdIn(Collection<UUID> accountIds);

    List<Operation> findByAccount_AccountIdAndOperationDateBetweenOrderByOperationDateAsc(
            UUID accountId, LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(o.operationAmount), 0) FROM Operation o " +
           "WHERE o.account.accountId = :accountId AND o.operationDate > :after")
    BigDecimal sumAmountByAccountAndDateAfter(@Param("accountId") UUID accountId,
                                              @Param("after") LocalDateTime after);
}
