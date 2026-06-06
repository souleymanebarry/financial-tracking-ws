package com.barry.bank.batch.statement.processor;

import com.barry.bank.batch.statement.pdf.StatementPdfResult;
import com.barry.bank.document.statement.StatementRenderer;
import com.barry.bank.document.statement.StatementData;
import com.barry.bank.document.statement.StatementLineData;
import com.barry.bank.domain.entities.BankStatement;
import com.barry.bank.domain.entities.StatementLine;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
@Log4j2
@RequiredArgsConstructor
public class BankStatementPdfProcessor implements ItemProcessor<BankStatement, StatementPdfResult> {

    private final StatementRenderer statementRenderer;

    @Override
    public StatementPdfResult process(@NonNull BankStatement statement) throws Exception {
        log.debug("Génération PDF — relevé: {}, compte: {}",
                statement.getId(), statement.getAccount().getAccountId());
        byte[] pdfBytes = statementRenderer.render(toStatementData(statement), Locale.FRENCH);
        return new StatementPdfResult(statement, pdfBytes);
    }

    private StatementData toStatementData(BankStatement statement) {
        var account  = statement.getAccount();
        var customer = account.getCustomer();

        List<StatementLineData> lines = statement.getLines().stream()
                .sorted(Comparator.comparing(StatementLine::getOperationDate))
                .map(line -> new StatementLineData(
                        line.getOperationDate(),
                        line.getOperationNumber(),
                        line.getLabel(),
                        line.getAmount(),
                        line.getOperationType(),
                        line.getRunningBalance()))
                .toList();

        return new StatementData(
                statement.getId(),
                account.getRib(),
                customer.getFirstName() + " " + customer.getLastName(),
                customer.getEmail(),
                statement.getPeriodStart(),
                statement.getPeriodEnd(),
                statement.getGeneratedAt(),
                statement.getOpeningBalance(),
                statement.getClosingBalance(),
                lines);
    }
}