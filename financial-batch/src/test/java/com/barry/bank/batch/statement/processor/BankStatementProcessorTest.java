package com.barry.bank.batch.statement.processor;

import com.barry.bank.domain.model.BankStatement;
import com.barry.bank.domain.model.CurrentAccount;
import com.barry.bank.domain.model.Customer;
import com.barry.bank.domain.model.Operation;
import com.barry.bank.domain.enumerations.OperationType;
import com.barry.bank.domain.enumerations.StatementStatus;
import com.barry.bank.persistence.repositories.BankStatementRepository;
import com.barry.bank.persistence.repositories.OperationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class BankStatementProcessorTest {

    @Mock private OperationRepository     operationRepository;
    @Mock private BankStatementRepository bankStatementRepository;

    private BankStatementProcessor processor;

    private static final LocalDate     PERIOD_START = LocalDate.of(2026, 5, 1);
    private static final LocalDate     PERIOD_END   = LocalDate.of(2026, 5, 31);
    private static final UUID          ACCOUNT_ID   = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final LocalDateTime FIXED_DATE   = LocalDateTime.of(2026, 5, 10, 9, 0);

    @BeforeEach
    void setUp() {
        processor = new BankStatementProcessor(operationRepository, bankStatementRepository);
        ReflectionTestUtils.setField(processor, "periodStartParam", PERIOD_START.toString());
        ReflectionTestUtils.setField(processor, "periodEndParam",   PERIOD_END.toString());
        ReflectionTestUtils.invokeMethod(processor, "parsePeriod");
        when(bankStatementRepository.findAccountIdsByPeriod(PERIOD_START, PERIOD_END))
                .thenReturn(List.of());
        processor.loadProcessedAccounts(mock(org.springframework.batch.core.StepExecution.class));
    }

    @Test
    void shouldReturnNullWhenStatementAlreadyExists() {
        // given — recharger le Set avec ACCOUNT_ID déjà traité
        when(bankStatementRepository.findAccountIdsByPeriod(PERIOD_START, PERIOD_END))
                .thenReturn(List.of(ACCOUNT_ID));
        processor.loadProcessedAccounts(mock(org.springframework.batch.core.StepExecution.class));

        // when
        BankStatement result = processor.process(account());

        // then
        assertThat(result).isNull();
        verify(operationRepository, never()).sumAmountByAccountAndDateAfter(any(), any());
        verify(operationRepository, never()).findByAccount_AccountIdAndOperationDateBetweenOrderByOperationDateAsc(any(), any(), any());
    }

    @Test
    void shouldComputeClosingBalanceBySubtractingOpsAfterPeriod() {
        // given — balance=1500, somme des ops après période = 500 → closing = 1500-500 = 1000
        CurrentAccount account = account(new BigDecimal("1500.00"));
        stubSumAfterPeriod(new BigDecimal("500.00"));
        stubPeriodOps(List.of());

        // when
        BankStatement result = processor.process(account);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getClosingBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldComputeOpeningBalanceBySubtractingPeriodOps() {
        // given — closing=1000 (balance=1000, rien après), ops période : +400, -100 → net=300
        //         opening = 1000 - 300 = 700
        CurrentAccount account = account(new BigDecimal("1000.00"));
        stubSumAfterPeriod(BigDecimal.ZERO);
        stubPeriodOps(List.of(
                op(new BigDecimal("400.00"),  OperationType.CREDIT),
                op(new BigDecimal("-100.00"), OperationType.DEBIT)
        ));

        // when
        BankStatement result = processor.process(account);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOpeningBalance()).isEqualByComparingTo("700.00");
        assertThat(result.getClosingBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldCreateStatementAsPending() {
        // given
        CurrentAccount account = account();
        stubSumAfterPeriod(BigDecimal.ZERO);
        stubPeriodOps(List.of());

        // when
        BankStatement result = processor.process(account);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(StatementStatus.PENDING);
        assertThat(result.getPeriodStart()).isEqualTo(PERIOD_START);
        assertThat(result.getPeriodEnd()).isEqualTo(PERIOD_END);
        assertThat(result.getAccount()).isSameAs(account);
        assertThat(result.getGeneratedAt()).isNotNull();
    }

    @Test
    void shouldBuildStatementLinesAsSnapshots() {
        // given — 2 ops en période
        CurrentAccount account = account();
        stubSumAfterPeriod(BigDecimal.ZERO);
        stubPeriodOps(List.of(
                op("OP-001", new BigDecimal("200.00"),  OperationType.CREDIT, "Virement",
                        LocalDateTime.of(2026, 5, 5, 10, 0)),
                op("OP-002", new BigDecimal("-50.00"),  OperationType.DEBIT,  "CB",
                        LocalDateTime.of(2026, 5, 12, 14, 0))
        ));

        // when
        BankStatement result = processor.process(account);

        // then — snapshot : label, amount, type copiés depuis l'opération
        assertThat(result).isNotNull();
        assertThat(result.getLines()).hasSize(2);
        assertThat(result.getLines().get(0).getLabel()).isEqualTo("Virement");
        assertThat(result.getLines().get(0).getAmount()).isEqualByComparingTo("200.00");
        assertThat(result.getLines().get(0).getOperationType()).isEqualTo(OperationType.CREDIT);
        assertThat(result.getLines().get(1).getLabel()).isEqualTo("CB");
        assertThat(result.getLines().get(1).getAmount()).isEqualByComparingTo("-50.00");
    }

    @Test
    void shouldCalculateRunningBalanceForEachLine() {
        // given — opening=600, ops : +300, -100, +200
        // running : 900 → 800 → 1000 (= closing)
        CurrentAccount account = account(new BigDecimal("1000.00"));
        stubSumAfterPeriod(BigDecimal.ZERO);
        stubPeriodOps(List.of(
                op(new BigDecimal("300.00"),  OperationType.CREDIT),
                op(new BigDecimal("-100.00"), OperationType.DEBIT),
                op(new BigDecimal("200.00"),  OperationType.CREDIT)
        ));

        // when
        BankStatement result = processor.process(account);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getLines().get(0).getRunningBalance()).isEqualByComparingTo("900.00");
        assertThat(result.getLines().get(1).getRunningBalance()).isEqualByComparingTo("800.00");
        assertThat(result.getLines().get(2).getRunningBalance()).isEqualByComparingTo("1000.00");
        assertThat(result.getLines().get(2).getRunningBalance())
                .isEqualByComparingTo(result.getClosingBalance());
    }

    @Test
    void shouldHandleNoOperationsInPeriod() {
        // given — aucune op dans la période ni après
        CurrentAccount account = account(new BigDecimal("2000.00"));
        stubSumAfterPeriod(BigDecimal.ZERO);
        stubPeriodOps(List.of());

        // when
        BankStatement result = processor.process(account);

        // then — opening = closing = balance, aucune ligne
        assertThat(result).isNotNull();
        assertThat(result.getOpeningBalance()).isEqualByComparingTo("2000.00");
        assertThat(result.getClosingBalance()).isEqualByComparingTo("2000.00");
        assertThat(result.getLines()).isEmpty();
    }

    @Test
    void shouldQueryRepositoriesWithCorrectBoundaries() {
        // given
        CurrentAccount account = account();
        stubSumAfterPeriod(BigDecimal.ZERO);
        stubPeriodOps(List.of());

        // when
        processor.process(account);

        // then — bornes exactes passées aux requêtes
        verify(operationRepository).sumAmountByAccountAndDateAfter(
                ACCOUNT_ID, PERIOD_END.atTime(LocalTime.MAX));

        verify(operationRepository).findByAccount_AccountIdAndOperationDateBetweenOrderByOperationDateAsc(
                ACCOUNT_ID, PERIOD_START.atStartOfDay(), PERIOD_END.atTime(LocalTime.MAX));
    }

    // -------------------------------------------------------------------------

    private CurrentAccount account() {
        return account(new BigDecimal("1000.00"));
    }

    private CurrentAccount account(BigDecimal balance) {
        return CurrentAccount.builder()
                .accountId(ACCOUNT_ID)
                .balance(balance)
                .customer(Customer.builder().firstName("Test").lastName("User").build())
                .overDraft(BigDecimal.ZERO)
                .build();
    }

    private Operation op(BigDecimal amount, OperationType type) {
        return op(UUID.randomUUID().toString(), amount, type, "Libellé", FIXED_DATE);
    }

    private Operation op(String number, BigDecimal amount, OperationType type,
                         String description, LocalDateTime date) {
        return Operation.builder()
                .operationNumber(number).operationAmount(amount)
                .operationType(type).description(description).operationDate(date)
                .build();
    }

    private void stubSumAfterPeriod(BigDecimal sum) {
        when(operationRepository.sumAmountByAccountAndDateAfter(
                any(UUID.class), any(LocalDateTime.class))).thenReturn(sum);
    }

    private void stubPeriodOps(List<Operation> ops) {
        when(operationRepository.findByAccount_AccountIdAndOperationDateBetweenOrderByOperationDateAsc(
                any(UUID.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(ops);
    }
}
