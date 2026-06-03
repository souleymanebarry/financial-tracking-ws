package com.barry.bank.batch.statement.reader;

import com.barry.bank.domain.entities.BankAccount;
import com.barry.bank.domain.entities.enums.AccountStatus;
import com.barry.bank.persistence.repositories.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;


import java.util.Iterator;
import java.util.List;


/**
 * Fournit les {@link BankAccount} éligibles à la génération de relevé.
 *
 * <p>Seuls les comptes {@link AccountStatus#ACTIVATED} sont traités —
 * les comptes {@code CREATED} (non encore activés) et {@code SUSPENDED} sont exclus.
 */
@Component
@StepScope
@Log4j2
@RequiredArgsConstructor
public class BankAccountItemReader implements ItemReader<BankAccount> {

    private final BankAccountRepository bankAccountRepository;

    private Iterator<BankAccount> iterator;

    @Override
    public BankAccount read() {
        if (iterator == null) {
            List<BankAccount> accounts = bankAccountRepository.findByStatus(AccountStatus.ACTIVATED);
            log.info("BankAccountItemReader — {} compte(s) ACTIVATED chargé(s)", accounts.size());
            iterator = accounts.iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }
}

