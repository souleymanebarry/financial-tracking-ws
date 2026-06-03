package com.barry.bank.batch.statement.job;

import com.barry.bank.batch.statement.processor.BankStatementProcessor;
import com.barry.bank.batch.statement.reader.BankAccountItemReader;
import com.barry.bank.batch.statement.writer.BankStatementWriter;
import com.barry.bank.domain.entities.BankAccount;
import com.barry.bank.domain.entities.BankStatement;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class BankStatementJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final BankAccountItemReader bankAccountItemReader;
    private final BankStatementProcessor bankStatementProcessor;
    private final BankStatementWriter bankStatementWriter;

    @Bean
    public Job generateMonthlyStatementsJob() {
        return new JobBuilder("generateMonthlyStatementsJob", jobRepository)
                .start(generateStatementStep())
                .build();
    }

    @Bean
    public Step generateStatementStep() {
        return new StepBuilder("generateStatementStep", jobRepository)
                .<BankAccount, BankStatement>chunk(10, transactionManager)
                .reader(bankAccountItemReader)
                .processor(bankStatementProcessor)
                .writer(bankStatementWriter)
                .build();
    }
}
