package com.barry.bank.application.services.impl;

import com.barry.bank.domain.entities.BankAccount;
import com.barry.bank.domain.entities.CurrentAccount;
import com.barry.bank.domain.entities.Operation;
import com.barry.bank.persistence.repositories.BankAccountRepository;
import com.barry.bank.persistence.repositories.OperationRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.barry.bank.domain.entities.enums.AccountStatus.ACTIVATED;
import static com.barry.bank.domain.entities.enums.OperationType.CREDIT;
import static com.barry.bank.domain.entities.enums.OperationType.DEBIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationServiceImplTest {

    @Mock
    private BankAccountRepository accountRepository;

    @Mock
    private OperationRepository operationRepository;

    @InjectMocks
    private OperationServiceImpl operationService;

    //----------------------------------DEBIT-----------------------------------

    @ParameterizedTest
    @MethodSource("invalidDebitInputs")
    void shouldThrowsExceptionForInvalidDebit(UUID accountId, BigDecimal amount, String description) {
        assertThatThrownBy(() -> operationService.debitAccount(accountId, amount, description))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AccountId must not be null and amount must be greater than zero");

        verify(accountRepository, never()).findById(any());
        verify(accountRepository, never()).save(any());
        verify(operationRepository, never()).save(any());
    }

    private static Stream<Arguments> invalidDebitInputs() {
        UUID validId = UUID.randomUUID();
        return Stream.of(
                Arguments.of(null, BigDecimal.valueOf(200), "desc"),
                Arguments.of(validId, null, "desc"),
                Arguments.of(validId, BigDecimal.valueOf(-2), "desc"),
                Arguments.of(validId, BigDecimal.ZERO, "desc")
        );
    }

    @Test
    void shouldThrowExceptionWhenAccountNotFoundOnDebit() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operationService.debitAccount(accountId, BigDecimal.valueOf(200), "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account not found with Id: " + accountId);

        verify(accountRepository, times(1)).findById(accountId);
        verify(operationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenAmountExceedsBalanceOnDebit() {
        UUID accountId = UUID.randomUUID();
        CurrentAccount account = CurrentAccount.builder()
                .accountId(accountId)
                .balance(BigDecimal.valueOf(20_000))
                .status(ACTIVATED)
                .overDraft(BigDecimal.valueOf(200)).build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> operationService.debitAccount(accountId, BigDecimal.valueOf(200_000), "desc"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient balance for a debit transaction");

        verify(accountRepository, times(1)).findById(accountId);
        verify(operationRepository, never()).save(any());
    }

    @Test
    void shouldDebitAccountAndRecordOperationWhenValid() {
        UUID accountId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(2_000);

        CurrentAccount account = CurrentAccount.builder()
                .accountId(accountId)
                .rib("FR76 1212 1254 3167 6565 374")
                .balance(BigDecimal.valueOf(200_000))
                .createdAt(LocalDateTime.now())
                .status(ACTIVATED)
                .overDraft(BigDecimal.valueOf(200)).build();

        ArgumentCaptor<BankAccount> accountCaptor = ArgumentCaptor.forClass(BankAccount.class);
        ArgumentCaptor<Operation> operationCaptor = ArgumentCaptor.forClass(Operation.class);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenReturn(account);

        operationService.debitAccount(accountId, amount, "Online purchase");

        verify(accountRepository).save(accountCaptor.capture());
        verify(operationRepository).save(operationCaptor.capture());

        BankAccount savedAccount = accountCaptor.getValue();
        Operation savedOperation = operationCaptor.getValue();

        assertAll(
                () -> assertThat(savedAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(198_000)),
                () -> assertThat(savedOperation.getOperationAmount()).isEqualByComparingTo(BigDecimal.valueOf(2_000)),
                () -> assertThat(savedOperation.getOperationDate()).isNotNull(),
                () -> assertThat(savedOperation.getOperationType()).isEqualTo(DEBIT),
                () -> assertThat(savedOperation.getAccount()).isSameAs(account),
                () -> assertThat(savedOperation.getDescription()).isEqualTo("Online purchase")
        );
    }

    //----------------------------------CREDIT-----------------------------------

    @ParameterizedTest
    @MethodSource("invalidCreditInputs")
    void shouldThrowExceptionForInvalidCredit(UUID accountId, BigDecimal amount, String description) {
        assertThatThrownBy(() -> operationService.creditAccount(accountId, amount, description))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AccountId or amount must not be null");

        verify(accountRepository, never()).findById(any());
        verify(accountRepository, never()).save(any());
        verify(operationRepository, never()).save(any());
    }

    private static Stream<Arguments> invalidCreditInputs() {
        UUID validId = UUID.randomUUID();
        return Stream.of(
                Arguments.of(null, BigDecimal.valueOf(2000), "desc"),
                Arguments.of(validId, null, "desc")
        );
    }

    @ParameterizedTest
    @CsvSource({
            "550e8400-e29b-41d4-a716-446655440000, 0, Credit transaction",
            "550e8400-e29b-41d4-a716-446655440000, -1, Credit transaction"
    })
    void shouldThrowExceptionWhenAmountLessOrEqualToZeroOnCredit(UUID accountId, BigDecimal amount, String description) {
        assertThatThrownBy(() -> operationService.creditAccount(accountId, amount, description))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credit amount must be greater than zero");

        verify(accountRepository, never()).findById(any());
        verify(accountRepository, never()).save(any());
        verify(operationRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenAccountNotFoundOnCredit() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operationService.creditAccount(accountId, BigDecimal.valueOf(200), "desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account not found with Id: " + accountId);

        verify(accountRepository, times(1)).findById(accountId);
        verify(accountRepository, never()).save(any());
        verify(operationRepository, never()).save(any());
    }

    @Test
    void shouldCreditAccountAndRecordOperationWhenValid() {
        UUID accountId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(2_000);

        CurrentAccount account = CurrentAccount.builder()
                .accountId(accountId)
                .rib("FR76 1111 3333 3167 6565 374")
                .balance(BigDecimal.valueOf(21_000))
                .createdAt(LocalDateTime.now())
                .status(ACTIVATED)
                .overDraft(BigDecimal.valueOf(200)).build();

        ArgumentCaptor<BankAccount> accountCaptor = ArgumentCaptor.forClass(BankAccount.class);
        ArgumentCaptor<Operation> operationCaptor = ArgumentCaptor.forClass(Operation.class);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenReturn(account);

        operationService.creditAccount(accountId, amount, "Salary payment");

        verify(accountRepository).save(accountCaptor.capture());
        verify(operationRepository).save(operationCaptor.capture());

        BankAccount savedAccount = accountCaptor.getValue();
        Operation savedOperation = operationCaptor.getValue();

        assertAll(
                () -> assertThat(savedAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(23_000)),
                () -> assertThat(savedOperation.getOperationAmount()).isEqualByComparingTo(BigDecimal.valueOf(2_000)),
                () -> assertThat(savedOperation.getOperationDate()).isNotNull(),
                () -> assertThat(savedOperation.getOperationType()).isEqualTo(CREDIT),
                () -> assertThat(savedOperation.getAccount()).isSameAs(account),
                () -> assertThat(savedOperation.getDescription()).isEqualTo("Salary payment")
        );
    }
}
