package com.barry.bank.batch;

import com.barry.bank.domain.entities.BankAccount;
import com.barry.bank.domain.entities.BankStatement;
import com.barry.bank.domain.entities.CurrentAccount;
import com.barry.bank.domain.entities.StatementLine;
import com.barry.bank.domain.entities.Customer;
import com.barry.bank.domain.entities.Operation;
import com.barry.bank.domain.entities.enums.AccountStatus;
import com.barry.bank.domain.entities.enums.Gender;
import com.barry.bank.domain.entities.enums.OperationType;
import com.barry.bank.domain.entities.enums.StatementStatus;
import com.barry.bank.persistence.repositories.BankAccountRepository;
import com.barry.bank.persistence.repositories.BankStatementRepository;
import com.barry.bank.persistence.repositories.CustomerRepository;
import com.barry.bank.persistence.repositories.OperationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class BankStatementJobIT {

    @MockBean MinioClient minioClient;

    @Autowired private JobLauncher  jobLauncher;
    @Autowired private JobRepository jobRepository;
    @Autowired private Job           generateMonthlyStatementsJob;

    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired private CustomerRepository      customerRepository;
    @Autowired private BankAccountRepository   bankAccountRepository;
    @Autowired private OperationRepository     operationRepository;
    @Autowired private BankStatementRepository bankStatementRepository;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils = new JobLauncherTestUtils();
        jobLauncherTestUtils.setJobLauncher(jobLauncher);
        jobLauncherTestUtils.setJobRepository(jobRepository);
        jobLauncherTestUtils.setJob(generateMonthlyStatementsJob);

        new JobRepositoryTestUtils(jobRepository).removeJobExecutions();
        bankStatementRepository.deleteAll();
        operationRepository.deleteAll();
        bankAccountRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    void shouldGenerateStatementAndPdf() throws Exception {
        // given
        LocalDate periodStart = LocalDate.of(2026, 5, 1);
        LocalDate periodEnd   = LocalDate.of(2026, 5, 31);

        CurrentAccount account = activatedAccount("Jean", "Dupont", "jean@test.com",
                "FR7630001007941234567890185", new BigDecimal("1200.00"));

        operationRepository.saveAll(List.of(
                op(account, "OP-001", new BigDecimal("500.00"),  OperationType.CREDIT,
                        LocalDateTime.of(2026, 5, 5,  10, 0), "Virement entrant"),
                op(account, "OP-002", new BigDecimal("-200.00"), OperationType.DEBIT,
                        LocalDateTime.of(2026, 5, 12, 14, 0), "Paiement CB"),
                op(account, "OP-003", new BigDecimal("-100.00"), OperationType.DEBIT,
                        LocalDateTime.of(2026, 5, 20,  9, 0), "Prélèvement EDF"),
                op(account, "OP-004", new BigDecimal("300.00"),  OperationType.CREDIT,
                        LocalDateTime.of(2026, 6, 2,   8, 0), "Salaire")  // hors période
        ));

        // when
        JobExecution execution = runJob(periodStart, periodEnd);

        // then — job
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions()).hasSize(2);

        // then — relevé GENERATED avec les bons soldes
        // Option A : balance=1200, opsAfterPériod=[OP-004=+300] → closing=900
        //            sumPériod=[+500-200-100]=+200             → opening=700
        List<BankStatement> statements = bankStatementRepository.findByStatusWithLines(StatementStatus.GENERATED);
        assertThat(statements).hasSize(1);

        BankStatement statement = statements.get(0);
        assertThat(statement.getClosingBalance()).isEqualByComparingTo("900.00");
        assertThat(statement.getOpeningBalance()).isEqualByComparingTo("700.00");
        assertThat(statement.getLines()).hasSize(3);

        // then — clé objet MinIO au bon format
        assertThat(statement.getFileUrl())
                .isNotNull()
                .startsWith("statements/2026/05/")
                .endsWith("_2026-05-01_2026-05-31.pdf");
    }

    @Test
    void shouldSkipSuspendedAccounts() throws Exception {
        // given — compte SUSPENDED
        Customer customer = customerRepository.save(Customer.builder()
                .firstName("Marie").lastName("Martin").email("marie@test.com")
                .gender(Gender.FEMALE).build());
        bankAccountRepository.save(CurrentAccount.builder()
                .rib("FR7630001007941234567890999")
                .balance(new BigDecimal("500.00")).status(AccountStatus.SUSPENDED)
                .customer(customer).createdAt(LocalDateTime.now()).overDraft(BigDecimal.ZERO)
                .build());

        // when
        JobExecution execution = runJob(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(bankStatementRepository.findAll()).isEmpty();
    }

    @Test
    void shouldBeIdempotent() throws Exception {
        // given
        activatedAccount("Paul", "Bernard", "paul@test.com",
                "FR7630001007941234567890777", new BigDecimal("1000.00"));

        // when — deux lancements sur la même période
        runJob(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));
        runJob(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        // then — un seul relevé, pas de doublon
        assertThat(bankStatementRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldGenerateStatementWithNoOperationsInPeriod() throws Exception {
        // given — compte actif, aucune opération dans la période
        activatedAccount("Alice", "Martin", "alice@test.com",
                "FR7630001007941234567890111", new BigDecimal("2000.00"));

        // when
        JobExecution execution = runJob(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        // then — relevé généré : opening = closing = balance courante, 0 lignes
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<BankStatement> statements = bankStatementRepository.findByStatusWithLines(StatementStatus.GENERATED);
        assertThat(statements).hasSize(1);

        BankStatement statement = statements.get(0);
        assertThat(statement.getOpeningBalance()).isEqualByComparingTo("2000.00");
        assertThat(statement.getClosingBalance()).isEqualByComparingTo("2000.00");
        assertThat(statement.getLines()).isEmpty();
    }

    @Test
    void shouldSkipCreatedAccounts() throws Exception {
        // given — compte CREATED (pas encore activé)
        Customer customer = customerRepository.save(Customer.builder()
                .firstName("Bob").lastName("Dupuis").email("bob@test.com")
                .gender(Gender.MALE).build());
        bankAccountRepository.save(CurrentAccount.builder()
                .rib("FR7630001007941234567890222")
                .balance(new BigDecimal("500.00")).status(AccountStatus.CREATED)
                .customer(customer).createdAt(LocalDateTime.now()).overDraft(BigDecimal.ZERO)
                .build());

        // when
        JobExecution execution = runJob(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(bankStatementRepository.findAll()).isEmpty();
    }

    @Test
    void shouldProcessMultipleAccountsIndependently() throws Exception {
        // given — 2 comptes ACTIVATED + 1 SUSPENDED
        CurrentAccount account1 = activatedAccount("Luc", "Blanc", "luc@test.com",
                "FR7630001007941234567890333", new BigDecimal("1000.00"));
        CurrentAccount account2 = activatedAccount("Eva", "Noir", "eva@test.com",
                "FR7630001007941234567890444", new BigDecimal("3000.00"));

        Customer suspended = customerRepository.save(Customer.builder()
                .firstName("Max").lastName("Gris").email("max@test.com")
                .gender(Gender.MALE).build());
        bankAccountRepository.save(CurrentAccount.builder()
                .rib("FR7630001007941234567890555")
                .balance(new BigDecimal("500.00")).status(AccountStatus.SUSPENDED)
                .customer(suspended).createdAt(LocalDateTime.now()).overDraft(BigDecimal.ZERO)
                .build());

        operationRepository.save(op(account1, "OP-A1", new BigDecimal("200.00"), OperationType.CREDIT,
                LocalDateTime.of(2026, 5, 10, 9, 0), "Virement A"));
        operationRepository.save(op(account2, "OP-B1", new BigDecimal("-500.00"), OperationType.DEBIT,
                LocalDateTime.of(2026, 5, 15, 14, 0), "Prélèvement B"));

        // when
        JobExecution execution = runJob(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        // then — 2 relevés (un par compte ACTIVATED), 0 pour le compte SUSPENDED
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<BankStatement> statements = bankStatementRepository.findByStatusWithLines(StatementStatus.GENERATED);
        assertThat(statements).hasSize(2);

        BankStatement stmt1 = statements.stream()
                .filter(s -> s.getAccount().getAccountId().equals(account1.getAccountId()))
                .findFirst().orElseThrow();
        assertThat(stmt1.getLines()).hasSize(1);
        assertThat(stmt1.getClosingBalance()).isEqualByComparingTo("1000.00");
        assertThat(stmt1.getOpeningBalance()).isEqualByComparingTo("800.00");

        BankStatement stmt2 = statements.stream()
                .filter(s -> s.getAccount().getAccountId().equals(account2.getAccountId()))
                .findFirst().orElseThrow();
        assertThat(stmt2.getLines()).hasSize(1);
        assertThat(stmt2.getClosingBalance()).isEqualByComparingTo("3000.00");
        assertThat(stmt2.getOpeningBalance()).isEqualByComparingTo("3500.00");
    }

    @Test
    void shouldCalculateRunningBalanceCorrectly() throws Exception {
        // given — balance=1000, aucune op après la période
        // ops en période : +300, -100, +200 → net=+400
        // closing = 1000 - 0 = 1000
        // opening = 1000 - 400 = 600
        // running : 600+300=900 → 900-100=800 → 800+200=1000 (= closing)
        CurrentAccount account = activatedAccount("Clara", "Bleu", "clara@test.com",
                "FR7630001007941234567890666", new BigDecimal("1000.00"));

        operationRepository.saveAll(List.of(
                op(account, "OP-1", new BigDecimal("300.00"),  OperationType.CREDIT,
                        LocalDateTime.of(2026, 5, 3, 9, 0),  "Virement"),
                op(account, "OP-2", new BigDecimal("-100.00"), OperationType.DEBIT,
                        LocalDateTime.of(2026, 5, 10, 14, 0), "Retrait"),
                op(account, "OP-3", new BigDecimal("200.00"),  OperationType.CREDIT,
                        LocalDateTime.of(2026, 5, 20, 11, 0), "Remboursement")
        ));

        // when
        runJob(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        // then
        BankStatement statement = bankStatementRepository
                .findByStatusWithLines(StatementStatus.GENERATED).get(0);

        assertThat(statement.getOpeningBalance()).isEqualByComparingTo("600.00");
        assertThat(statement.getClosingBalance()).isEqualByComparingTo("1000.00");

        List<StatementLine> lines = statement.getLines().stream()
                .sorted(java.util.Comparator.comparing(StatementLine::getOperationDate))
                .toList();

        assertThat(lines).hasSize(3);
        assertThat(lines.get(0).getRunningBalance()).isEqualByComparingTo("900.00");
        assertThat(lines.get(1).getRunningBalance()).isEqualByComparingTo("800.00");
        assertThat(lines.get(2).getRunningBalance()).isEqualByComparingTo("1000.00");

        // le dernier runningBalance doit être égal au closingBalance
        assertThat(lines.get(2).getRunningBalance())
                .isEqualByComparingTo(statement.getClosingBalance());
    }

    // -------------------------------------------------------------------------

    private CurrentAccount activatedAccount(String firstName, String lastName, String email,
                                            String rib, BigDecimal balance) {
        Customer customer = customerRepository.save(Customer.builder()
                .firstName(firstName).lastName(lastName).email(email)
                .gender(Gender.MALE).build());
        return bankAccountRepository.save(CurrentAccount.builder()
                .rib(rib).balance(balance).status(AccountStatus.ACTIVATED)
                .customer(customer).createdAt(LocalDateTime.now()).overDraft(BigDecimal.ZERO)
                .build());
    }

    private Operation op(BankAccount account, String number, BigDecimal amount,
                         OperationType type, LocalDateTime date, String description) {
        return Operation.builder()
                .operationNumber(number).operationAmount(amount)
                .operationType(type).operationDate(date)
                .description(description).account(account).build();
    }

    private JobExecution runJob(LocalDate periodStart, LocalDate periodEnd) throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString("periodStart", periodStart.toString())
                .addString("periodEnd",   periodEnd.toString())
                .addLong("timestamp",     System.currentTimeMillis())
                .toJobParameters();
        return jobLauncherTestUtils.launchJob(params);
    }
}
