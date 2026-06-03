package com.barry.bank.batch.statement.reader;

import com.barry.bank.domain.entities.BankStatement;
import com.barry.bank.domain.entities.enums.StatementStatus;
import com.barry.bank.persistence.repositories.BankStatementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

/**
 * Charge les {@link BankStatement} en état {@link StatementStatus#PENDING} avec leurs lignes
 * (JOIN FETCH) pour éviter le LazyInitializationException dans le processor PDF.
 */
@Component
@StepScope
@Log4j2
@RequiredArgsConstructor
public class PendingStatementItemReader implements ItemReader<BankStatement> {

    private final BankStatementRepository bankStatementRepository;

    private Iterator<BankStatement> iterator;

    @Override
    public BankStatement read() {
        if (iterator == null) {
            List<BankStatement> statements = bankStatementRepository.findByStatusWithLines(StatementStatus.PENDING);
            log.info("PendingStatementItemReader — {} relevé(s) PENDING chargé(s)", statements.size());
            iterator = statements.iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }
}
