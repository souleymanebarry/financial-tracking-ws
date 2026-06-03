package com.barry.bank.persistence.repositories;

import com.barry.bank.domain.entities.BankStatement;
import com.barry.bank.domain.entities.enums.StatementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankStatementRepository extends JpaRepository<BankStatement, UUID> {

    List<BankStatement> findByAccount_AccountId(UUID accountId);

    List<BankStatement> findByStatus(StatementStatus status);

    Optional<BankStatement> findByAccount_AccountIdAndPeriodStartAndPeriodEnd(
            UUID accountId, LocalDate periodStart, LocalDate periodEnd);

    boolean existsByAccount_AccountIdAndPeriodStartAndPeriodEnd(
            UUID accountId, LocalDate periodStart, LocalDate periodEnd);
}
