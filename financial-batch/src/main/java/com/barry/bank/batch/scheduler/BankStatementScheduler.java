package com.barry.bank.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Déclenche le job de génération des relevés bancaires mensuels selon un cron configurable
 * via la propriété {@code batch.statement.cron}.
 *
 * <p>La période traitée est calculée automatiquement : le mois précédant la date d'exécution
 * (ex. le 1er juin → relevés du 1er au 31 mai).
 *
 * <p>Protégé contre les exécutions concurrentes : si le job est déjà en cours,
 * le déclenchement est ignoré. La vérification s'appuie sur les tables metadata
 * Spring Batch ({@link JobExplorer}) — aucun lock externe nécessaire.
 */
@Component
@Log4j2
@RequiredArgsConstructor
public class BankStatementScheduler {

    private static final String JOB_NAME = "generateMonthlyStatementsJob";

    private final JobLauncher  jobLauncher;
    private final JobExplorer  jobExplorer;
    private final Job          generateMonthlyStatementsJob;

    /**
     * Point d'entrée du scheduling. Calcule la période du mois précédent et lance le job
     * si aucune exécution n'est déjà active.
     */
    @Scheduled(cron = "${batch.statement.cron}")
    public void runMonthlyStatementJob() {
        if (isJobAlreadyRunning()) {
            log.warn("Batch relevé mensuel déjà en cours — déclenchement ignoré");
            return;
        }

        LocalDate periodStart = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDate periodEnd   = LocalDate.now().withDayOfMonth(1).minusDays(1);

        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("periodStart", periodStart.toString())
                    .addString("periodEnd",   periodEnd.toString())
                    .addLong("timestamp",     System.currentTimeMillis())
                    .toJobParameters();

            log.info("Lancement du batch relevé mensuel — période: {} → {}", periodStart, periodEnd);
            jobLauncher.run(generateMonthlyStatementsJob, params);

        } catch (Exception e) {
            log.error("Erreur lors du lancement du batch relevé mensuel: {}", e.getMessage(), e);
        }
    }

    private boolean isJobAlreadyRunning() {
        return !jobExplorer.findRunningJobExecutions(JOB_NAME).isEmpty();
    }
}
