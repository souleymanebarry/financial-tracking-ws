package com.barry.bank.batch.statement.reader;

import com.barry.bank.domain.entities.BankAccount;
import com.barry.bank.domain.entities.enums.AccountStatus;
import com.barry.bank.persistence.repositories.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Iterator;

/**
 * Fournit les {@link BankAccount} éligibles à la génération de relevé par pages de
 * {@value PAGE_SIZE} comptes — évite un OutOfMemoryError avec un grand nombre de comptes.
 *
 * <p>Seuls les comptes {@link AccountStatus#ACTIVATED} sont traités —
 * les comptes {@code CREATED} et {@code SUSPENDED} sont exclus.
 */
@Component
@StepScope
@Log4j2
@RequiredArgsConstructor
public class BankAccountItemReader implements ItemReader<BankAccount> {

    private static final int PAGE_SIZE = 100;

    private final BankAccountRepository bankAccountRepository;

    private int page = 0;
    private Iterator<BankAccount> iterator;

    @Override
    public BankAccount read() {
        if (iterator == null || !iterator.hasNext()) {
            var result = bankAccountRepository.findByStatus(
                    AccountStatus.ACTIVATED,
                    PageRequest.of(page++, PAGE_SIZE, Sort.by("accountId")));
            if (result.isEmpty()) return null;
            log.info("BankAccountItemReader — page {}, {} compte(s) ACTIVATED chargé(s)",
                    page - 1, result.getNumberOfElements());
            iterator = result.iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }
}
