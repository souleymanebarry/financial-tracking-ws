package com.barry.bank.batch.controller.trigger;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Endpoint de déclenchement manuel du batch — usage local uniquement.
 *
 * <pre>
 * POST /batch/statements/trigger?periodStart=2026-05-01&periodEnd=2026-05-31
 * </pre>
 */
@RestController
@RequestMapping("/batch/statements")
@Log4j2
@RequiredArgsConstructor
public class BatchTriggerController {

    private final JobLauncher jobLauncher;
    private final Job         generateMonthlyStatementsJob;

    @PostMapping("/trigger")
    public ResponseEntity<String> trigger(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {

        if (periodStart.isAfter(periodEnd)) {
            return ResponseEntity.badRequest()
                    .body("periodStart (%s) doit être avant periodEnd (%s)".formatted(periodStart, periodEnd));
        }
        if (periodEnd.isAfter(LocalDate.now())) {
            return ResponseEntity.badRequest()
                    .body("periodEnd (%s) ne peut pas être dans le futur".formatted(periodEnd));
        }

        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("periodStart", periodStart.toString())
                    .addString("periodEnd",   periodEnd.toString())
                    .addLong("timestamp",     System.currentTimeMillis())
                    .toJobParameters();

            log.info("Déclenchement manuel — période: {} → {}", periodStart, periodEnd);
            JobExecution execution = jobLauncher.run(generateMonthlyStatementsJob, params);

            return ResponseEntity.ok(
                    "Job terminé — id: %d, statut: %s".formatted(execution.getJobId(), execution.getStatus()));

        } catch (Exception e) {
            log.error("Erreur lors du déclenchement manuel: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Erreur: " + e.getMessage());
        }
    }
}
