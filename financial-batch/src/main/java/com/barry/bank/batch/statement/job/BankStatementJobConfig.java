package com.barry.bank.batch.statement.job;

import com.barry.bank.batch.statement.pdf.StatementPdfResult;
import com.barry.bank.batch.statement.processor.BankStatementPdfProcessor;
import com.barry.bank.batch.statement.processor.BankStatementProcessor;
import com.barry.bank.batch.statement.reader.BankAccountItemReader;
import com.barry.bank.batch.statement.reader.PendingStatementItemReader;
import com.barry.bank.batch.statement.writer.BankStatementPdfWriter;
import com.barry.bank.batch.statement.writer.BankStatementWriter;
import com.barry.bank.domain.model.BankAccount;
import com.barry.bank.domain.model.BankStatement;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class BankStatementJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Value("${batch.chunk.statement:10}")
    private int chunkStatement;

    @Value("${batch.chunk.pdf:5}")
    private int chunkPdf;

    // Step 1 — génération des relevés (PENDING)
    private final BankAccountItemReader bankAccountItemReader;
    private final BankStatementProcessor bankStatementProcessor;
    private final BankStatementWriter bankStatementWriter;

    // Step 2 — génération PDF (PENDING → GENERATED)
    private final PendingStatementItemReader pendingStatementItemReader;
    private final BankStatementPdfProcessor bankStatementPdfProcessor;
    private final BankStatementPdfWriter bankStatementPdfWriter;

    @Bean
    public Job generateMonthlyStatementsJob() {
        return new JobBuilder("generateMonthlyStatementsJob", jobRepository)
                .start(generateStatementStep())
                .next(generatePdfStep())
                .build();
    }

    @Bean
    public Step generateStatementStep() {
        return new StepBuilder("generateStatementStep", jobRepository)
                .<BankAccount, BankStatement>chunk(chunkStatement, transactionManager)
                .reader(bankAccountItemReader)
                .processor(bankStatementProcessor)
                .writer(bankStatementWriter)
                .build();
    }

    @Bean
    public Step generatePdfStep() {
        return new StepBuilder("generatePdfStep", jobRepository)
                .<BankStatement, StatementPdfResult>chunk(chunkPdf, transactionManager)
                .reader(pendingStatementItemReader)
                .processor(bankStatementPdfProcessor)
                .writer(bankStatementPdfWriter)
                .build();
    }
}
