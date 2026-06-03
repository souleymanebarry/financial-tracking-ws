package com.barry.bank.persistence.repositories;

import com.barry.bank.domain.entities.BankStatement;
import com.barry.bank.domain.entities.enums.StatementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BankStatementRepository extends JpaRepository<BankStatement, UUID> {

    // JOIN FETCH pour éviter le LazyInitializationException dans le processor PDF
    @Query("SELECT DISTINCT s FROM BankStatement s LEFT JOIN FETCH s.lines WHERE s.status = :status")
    List<BankStatement> findByStatusWithLines(@Param("status") StatementStatus status);

    boolean existsByAccount_AccountIdAndPeriodStartAndPeriodEnd(
            UUID accountId, LocalDate periodStart, LocalDate periodEnd);
}
