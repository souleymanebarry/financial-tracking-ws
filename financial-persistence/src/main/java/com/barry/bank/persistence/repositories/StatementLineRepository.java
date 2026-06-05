package com.barry.bank.persistence.repositories;

import com.barry.bank.domain.entities.StatementLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StatementLineRepository extends JpaRepository<StatementLine, UUID> {
}
