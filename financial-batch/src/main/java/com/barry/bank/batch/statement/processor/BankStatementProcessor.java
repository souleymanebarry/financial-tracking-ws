package com.barry.bank.batch.statement.processor;

import com.barry.bank.domain.model.BankAccount;
import com.barry.bank.domain.model.BankStatement;
import com.barry.bank.domain.model.Operation;
import com.barry.bank.domain.model.StatementLine;
import com.barry.bank.domain.enumerations.StatementStatus;
import com.barry.bank.persistence.repositories.BankStatementRepository;
import com.barry.bank.persistence.repositories.OperationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Calcule les soldes et construit les lignes d'un {@link BankStatement} pour chaque {@link BankAccount}.
 *
 * <p>Algorithme Option A — remontée depuis le solde courant :
 * <ul>
 *   <li>{@code closingBalance = account.balance − Σ(opérations strictement après periodEnd)}</li>
 *   <li>{@code openingBalance = closingBalance − Σ(opérations de la période)}</li>
 * </ul>
 *
 * <p>Idempotent : retourne {@code null} si un relevé existe déjà pour la période donnée,
 * ce qui permet de relancer le job sans créer de doublons (Spring Batch ignore les {@code null}).
 *
 * <p>Les {@link StatementLine} sont des snapshots immuables des {@link Operation} au moment
 * de la génération — une correction ultérieure d'opération ne modifie pas le relevé produit.
 */
@Component
@StepScope
@Log4j2
@RequiredArgsConstructor
public class BankStatementProcessor implements ItemProcessor<BankAccount, BankStatement> {

    private final OperationRepository operationRepository;
    private final BankStatementRepository bankStatementRepository;

    @Value("#{jobParameters['periodStart']}")
    private String periodStartParam;

    @Value("#{jobParameters['periodEnd']}")
    private String periodEndParam;

    private LocalDate    periodStart;
    private LocalDate    periodEnd;
    private Set<UUID>    processedAccountIds = Set.of();

    @PostConstruct
    private void parsePeriod() {
        periodStart = LocalDate.parse(periodStartParam);
        periodEnd   = LocalDate.parse(periodEndParam);
    }

    @BeforeStep
    public void loadProcessedAccounts(StepExecution stepExecution) {
        processedAccountIds = new HashSet<>(
                bankStatementRepository.findAccountIdsByPeriod(periodStart, periodEnd));
        log.info("Idempotence — {} relevé(s) déjà généré(s) pour la période {} → {}",
                processedAccountIds.size(), periodStart, periodEnd);
    }

    /**
     * @param account le compte à traiter, fourni par le {@link com.barry.bank.batch.statement.reader.BankAccountItemReader}
     * @return le relevé en état {@link StatementStatus#PENDING} avec ses lignes snapshot,
     *         ou {@code null} si un relevé existe déjà pour cette période
     */
    @Override
    public BankStatement process(@NonNull BankAccount account) {
        if (statementAlreadyExists(account)) {
            log.warn("Relevé déjà existant — compte: {}, période: {} → {}, ignoré",
                    account.getAccountId(), periodStart, periodEnd);
            return null;
        }

        List<Operation> periodOps = fetchPeriodOperations(account);
        BigDecimal      closing   = computeClosingBalance(account);
        BigDecimal      opening   = closing.subtract(sumOf(periodOps));

        log.debug("Relevé — compte: {}, période: {} → {}, ouverture: {}, clôture: {}, {} opération(s)",
                account.getAccountId(), periodStart, periodEnd, opening, closing, periodOps.size());

        BankStatement statement = buildStatement(account, opening, closing);
        attachLines(statement, periodOps, opening);
        return statement;
    }

    private boolean statementAlreadyExists(BankAccount account) {
        return processedAccountIds.contains(account.getAccountId());
    }

    private List<Operation> fetchPeriodOperations(BankAccount account) {
        return operationRepository.findByAccount_AccountIdAndOperationDateBetweenOrderByOperationDateAsc(
                account.getAccountId(),
                periodStart.atStartOfDay(),
                periodEnd.atTime(LocalTime.MAX));
    }

    private BigDecimal computeClosingBalance(BankAccount account) {
        BigDecimal sumAfter = operationRepository.sumAmountByAccountAndDateAfter(
                account.getAccountId(), periodEnd.atTime(LocalTime.MAX));
        return account.getBalance().subtract(sumAfter);
    }

    private BankStatement buildStatement(BankAccount account, BigDecimal opening, BigDecimal closing) {
        return BankStatement.builder()
                .account(account)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .openingBalance(opening)
                .closingBalance(closing)
                .status(StatementStatus.PENDING)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private void attachLines(BankStatement statement, List<Operation> ops, BigDecimal openingBalance) {
        BigDecimal running = openingBalance;
        for (Operation op : ops) {
            running = running.add(op.getOperationAmount());
            statement.getLines().add(toStatementLine(statement, op, running));
        }
    }

    private StatementLine toStatementLine(BankStatement statement, Operation op, BigDecimal runningBalance) {
        return StatementLine.builder()
                .statement(statement)
                .operationNumber(op.getOperationNumber())
                .operationDate(op.getOperationDate())
                .label(op.getDescription())
                .amount(op.getOperationAmount())
                .operationType(op.getOperationType())
                .runningBalance(runningBalance)
                .build();
    }

    private BigDecimal sumOf(List<Operation> operations) {
        return operations.stream()
                .map(Operation::getOperationAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
