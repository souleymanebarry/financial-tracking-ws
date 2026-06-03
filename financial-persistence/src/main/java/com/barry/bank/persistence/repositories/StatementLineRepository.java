package com.barry.bank.persistence.repositories;

import com.barry.bank.domain.entities.StatementLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StatementLineRepository extends JpaRepository<StatementLine, UUID> {

    List<StatementLine> findByStatement_IdOrderByOperationDateAsc(UUID statementId);
}
