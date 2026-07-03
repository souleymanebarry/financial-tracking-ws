package com.barry.bank.persistence.repositories;

import com.barry.bank.domain.model.BankStatement;
import com.barry.bank.domain.enumerations.StatementStatus;
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

    // Passe 2 : chargement complet par IDs — JOIN FETCH nécessaire car les statements
    // traversent plusieurs chunks et sont détachés entre les transactions de chunk
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


    @Query("SELECT s.account.accountId FROM BankStatement s " +
           "WHERE s.periodStart = :start AND s.periodEnd = :end")
    List<UUID> findAccountIdsByPeriod(@Param("start") LocalDate start,
                                      @Param("end") LocalDate end);
}
