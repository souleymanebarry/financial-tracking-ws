package com.barry.bank.persistence.repositories;

import com.barry.bank.domain.entities.BankStatement;
import com.barry.bank.domain.entities.enums.StatementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BankStatementRepository extends JpaRepository<BankStatement, UUID> {

    // Passe 1 : IDs paginés sans JOIN FETCH — évite le HHH90003004 de Hibernate
    @Query("SELECT s.id FROM BankStatement s WHERE s.status = :status ORDER BY s.generatedAt ASC")
    Page<UUID> findIdsByStatus(@Param("status") StatementStatus status, Pageable pageable);

    // Passe 2 : chargement complet par IDs avec JOIN FETCH
    @Query("""
            SELECT DISTINCT s FROM BankStatement s
            LEFT JOIN FETCH s.lines
            JOIN FETCH s.account a
            JOIN FETCH a.customer
            WHERE s.id IN :ids
            """)
    List<BankStatement> findByIdsWithDetails(@Param("ids") List<UUID> ids);

    // Utilisé dans les tests uniquement
    @Query("""
            SELECT DISTINCT s FROM BankStatement s
            LEFT JOIN FETCH s.lines
            JOIN FETCH s.account a
            JOIN FETCH a.customer
            WHERE s.status = :status
            """)
    List<BankStatement> findByStatusWithLines(@Param("status") StatementStatus status);

    boolean existsByAccount_AccountIdAndPeriodStartAndPeriodEnd(
            UUID accountId, LocalDate periodStart, LocalDate periodEnd);
}
