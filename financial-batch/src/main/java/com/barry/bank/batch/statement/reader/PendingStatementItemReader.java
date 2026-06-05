package com.barry.bank.batch.statement.reader;

import com.barry.bank.domain.entities.BankStatement;
import com.barry.bank.domain.entities.enums.StatementStatus;
import com.barry.bank.persistence.repositories.BankStatementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Fournit les {@link BankStatement} en état {@link StatementStatus#PENDING} par pages de
 * {@value PAGE_SIZE} relevés — évite un OutOfMemoryError avec un grand volume.
 *
 * <p>Approche 2 passes pour combiner pagination et JOIN FETCH sans déclencher le
 * warning Hibernate HHH90003004 (in-memory pagination) :
 * <ol>
 *   <li>Passe 1 — récupère les IDs paginés (requête légère, pas de JOIN FETCH)</li>
 *   <li>Passe 2 — charge les entités complètes par IDs avec JOIN FETCH</li>
 * </ol>
 */
@Component
@StepScope
@Log4j2
@RequiredArgsConstructor
public class PendingStatementItemReader implements ItemReader<BankStatement> {

    private static final int PAGE_SIZE = 50;

    private final BankStatementRepository bankStatementRepository;

    private int page = 0;
    private Iterator<BankStatement> iterator;

    @Override
    public BankStatement read() {
        if (iterator == null || !iterator.hasNext()) {
            List<UUID> ids = bankStatementRepository
                    .findIdsByStatus(StatementStatus.PENDING,
                            PageRequest.of(page++, PAGE_SIZE, Sort.by("generatedAt")))
                    .getContent();
            if (ids.isEmpty()) return null;
            List<BankStatement> statements = bankStatementRepository.findByIdsWithDetails(ids);
            log.info("PendingStatementItemReader — page {}, {} relevé(s) PENDING chargé(s)",
                    page - 1, statements.size());
            iterator = statements.iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }
}
