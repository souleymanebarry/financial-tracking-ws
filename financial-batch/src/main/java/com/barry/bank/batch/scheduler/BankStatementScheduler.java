package com.barry.bank.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@EnableScheduling
@Log4j2
@RequiredArgsConstructor
public class BankStatementScheduler {

    private final JobLauncher jobLauncher;

    private final Job generateMonthlyStatementsJob;

    @Scheduled(cron = "${batch.statement.cron}")
    public void runMonthlyStatementJob() {
        LocalDate today      = LocalDate.now();
        LocalDate periodStart = today.minusMonths(1).withDayOfMonth(1);
        LocalDate periodEnd   = today.withDayOfMonth(1).minusDays(1);

        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("periodStart", periodStart.toString())
                    .addString("periodEnd", periodEnd.toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            log.info("Lancement du batch relevé mensuel — période: {} → {}", periodStart, periodEnd);
            jobLauncher.run(generateMonthlyStatementsJob, params);

        } catch (Exception e) {
            log.error("Erreur lors du lancement du batch relevé mensuel: {}", e.getMessage(), e);
        }
    }
}
