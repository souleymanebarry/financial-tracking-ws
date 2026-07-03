package com.barry.bank.application.services.impl;

import com.barry.bank.domain.model.BankAccount;
import com.barry.bank.domain.model.CurrentAccount;
import com.barry.bank.domain.model.Customer;
import com.barry.bank.domain.model.Operation;
import com.barry.bank.domain.model.SavingAccount;
import com.barry.bank.application.services.OperationService;
import com.barry.bank.domain.exception.BusinessRuleException;
import com.barry.bank.domain.exception.ResourceNotFoundException;
import com.barry.bank.persistence.repositories.BankAccountRepository;
import com.barry.bank.persistence.repositories.CustomerRepository;
import com.barry.bank.persistence.repositories.OperationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import static java.util.Arrays.asList;

import static com.barry.bank.domain.enumerations.AccountStatus.ACTIVATED;
import static com.barry.bank.domain.enumerations.AccountStatus.CREATED;
import static com.barry.bank.domain.enumerations.Gender.MALE;
import static com.barry.bank.domain.enumerations.OperationType.DEBIT;
import static com.barry.bank.domain.enumerations.OperationType.CREDIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceImplTest {

    @Mock
    private BankAccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private OperationRepository operationRepository;

    @Mock
    private OperationService operationService;

    @InjectMocks
    private BankAccountServiceImpl accountService;

    Customer customer;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .customerId(UUID.randomUUID())
                .firstName("Alexandre")
                .lastName("DURANT")
                .gender(MALE)
                .email("alexandre.durant@gmail.com")
                .build();
    }

    @Test
    void shouldThrowExceptionWhenCreateCurrentAccountWithoutOverdraft() {
        //Arrange
        CurrentAccount account = CurrentAccount.builder()
                .accountId(UUID.randomUUID())
                .overDraft(null).build();

        //Act + Assert
        assertThatThrownBy(()->accountService.createCurrentAccount(account, customer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CurrentAccount must have an overDraft");

        //verify
        verify(customerRepository, never()).existsById(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenCustomerIdDoesNotExistOnCreateCurrentAccount() {
        //Arrange
        CurrentAccount account = CurrentAccount.builder()
                .accountId(UUID.randomUUID())
                .overDraft(BigDecimal.valueOf(150)).build();

        when(customerRepository.existsById(customer.getCustomerId())).thenReturn(false);

        //Act Assert
        assertThatThrownBy(()-> accountService.createCurrentAccount(account, customer))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found with ID: " + customer.getCustomerId());

        //verify
        verify(customerRepository, times(1)).existsById(any(UUID.class));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void shouldCreateCurrentAccountOnCreateCurrentAccount() {
        //Arrange
        CurrentAccount account = CurrentAccount.builder()
                .accountId(UUID.randomUUID())
                .rib("FR76 6175 0000 3167 7552 374")
                .balance(BigDecimal.valueOf(20_000))
                .createdAt(LocalDateTime.now())
                .status(CREATED)
                .overDraft(BigDecimal.valueOf(150)).build();

        when(customerRepository.existsById(customer.getCustomerId())).thenReturn(true);
        when(accountRepository.save(any(CurrentAccount.class))).thenReturn(account);

        //Act
        CurrentAccount result = accountService.createCurrentAccount(account, customer);

        assertAll(
                () -> assertThat(result.getAccountId()).isEqualTo(account.getAccountId()),
                () -> assertThat(result.getRib()).isNotBlank(),
                () -> assertThat(result.getBalance()).isEqualTo(BigDecimal.valueOf(20_000)),
                () -> assertThat(result.getOverDraft()).isEqualTo(account.getOverDraft()),
                () -> assertThat(result.getStatus()).isEqualTo(CREATED),
                () -> assertThat(result.getCustomer()).isEqualTo(customer),
                () -> assertThat(result.getCreatedAt()).isEqualTo(account.getCreatedAt())
        );

        //verify
        verify(customerRepository, times(1)).existsById(any(UUID.class));
        verify(accountRepository).save(account);
    }

    @Test
    void shouldCreateCurrentAccountWithDefaultValuesWhenBalanceOrCreatedAtIsNull() {
        //Arrange
        CurrentAccount account = CurrentAccount.builder()
                .accountId(UUID.randomUUID())
                .balance(null) // balance null
                .createdAt(null) // createdAt null
                .status(CREATED)
                .overDraft(BigDecimal.valueOf(200)).build();

        when(customerRepository.existsById(customer.getCustomerId())).thenReturn(true);
        when(accountRepository.save(any(CurrentAccount.class))).thenReturn(account);

        //Act
        CurrentAccount result = accountService.createCurrentAccount(account, customer);

        assertAll(
                () -> assertThat(result.getAccountId()).isEqualTo(account.getAccountId()),
                () -> assertThat(result.getRib()).isNotBlank(),
                () -> assertThat(result.getBalance()).isEqualTo(BigDecimal.ZERO),
                () -> assertThat(result.getOverDraft()).isEqualTo(account.getOverDraft()),
                () -> assertThat(result.getStatus()).isEqualTo(CREATED),
                () -> assertThat(result.getCustomer()).isEqualTo(customer),
                () -> assertThat(result.getCreatedAt()).isEqualTo(account.getCreatedAt())
        );

        //verify
        verify(customerRepository, times(1)).existsById(any(UUID.class));
        verify(accountRepository,times(1)).save(account);
    }


    // ----------------------------------SAVING ACCOUNT--------------------------

    @Test
    void shouldThrowExceptionWhenCreateSavingAccountWithoutInterestRate() {
        //Arrange
        SavingAccount account = SavingAccount.builder()
                .interestRate(null).build();

        //Act + Assert
        assertThatThrownBy(()->accountService.createSavingAccount(account, customer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("SavingAccount must have an InterestRate");

        //verify
        verify(customerRepository, never()).existsById(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenCustomerIdDoesNotExistOnCreateSavingAccount() {
        //Arrange
        SavingAccount account = SavingAccount.builder()
                .accountId(UUID.randomUUID())
                .interestRate(BigDecimal.valueOf(2.7)).build();

        when(customerRepository.existsById(customer.getCustomerId())).thenReturn(false);

        //Act Assert
        assertThatThrownBy(()-> accountService.createSavingAccount(account, customer))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found with ID: " + customer.getCustomerId());

        //verify
        verify(customerRepository, times(1)).existsById(any(UUID.class));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void shouldCreateSavingAccountOnCreateSavingAccount() {
        //Arrange
        SavingAccount account = SavingAccount.builder()
                .accountId(UUID.randomUUID())
                .rib("FR76 9999 1254 3167 7552 374")
                .balance(BigDecimal.valueOf(20_000))
                .createdAt(LocalDateTime.now())
                .status(CREATED)
                .interestRate(BigDecimal.valueOf(2.5)).build();

        when(customerRepository.existsById(customer.getCustomerId())).thenReturn(true);
        when(accountRepository.save(any(SavingAccount.class))).thenReturn(account);

        //Act
        SavingAccount result = accountService.createSavingAccount(account, customer);

        assertAll(
                () -> assertThat(result.getAccountId()).isEqualTo(account.getAccountId()),
                () -> assertThat(result.getRib()).isNotBlank(),
                () -> assertThat(result.getBalance()).isEqualTo(BigDecimal.valueOf(20_000)),
                () -> assertThat(result.getInterestRate()).isEqualTo(BigDecimal.valueOf(2.5)),
                () -> assertThat(result.getStatus()).isEqualTo(CREATED),
                () -> assertThat(result.getCustomer()).isEqualTo(customer),
                () -> assertThat(result.getCreatedAt()).isEqualTo(account.getCreatedAt())
        );

        //verify
        verify(customerRepository, times(1)).existsById(any(UUID.class));
        verify(accountRepository).save(account);
    }

    @Test
    void shouldCreateSavingAccountWithDefaultValuesWhenBalanceOrCreatedAtIsNull() {
        //Arrange
        SavingAccount account = SavingAccount.builder()
                .accountId(UUID.randomUUID())
                .rib("FR76 6175 1254 3333 7552 374")
                .balance(null) // balance null
                .createdAt(null) // createdAt null
                .status(CREATED)
                .interestRate(BigDecimal.valueOf(1.6)).build();

        when(customerRepository.existsById(customer.getCustomerId())).thenReturn(true);
        when(accountRepository.save(any(SavingAccount.class))).thenReturn(account);

        //Act
        SavingAccount result = accountService.createSavingAccount(account, customer);

        assertAll(
                () -> assertThat(result.getAccountId()).isEqualTo(account.getAccountId()),
                () -> assertThat(result.getRib()).isNotBlank(),
                () -> assertThat(result.getBalance()).isEqualTo(BigDecimal.ZERO),
                () -> assertThat(result.getInterestRate()).isEqualTo(BigDecimal.valueOf(1.6)),
                () -> assertThat(result.getStatus()).isEqualTo(CREATED),
                () -> assertThat(result.getCustomer()).isEqualTo(customer),
                () -> assertThat(result.getCreatedAt()).isEqualTo(account.getCreatedAt())
        );

        //verify
        verify(customerRepository, times(1)).existsById(any(UUID.class));
        verify(accountRepository,times(1)).save(account);
    }

    //----------------------------------TRANSFER-----------------------------------

    @ParameterizedTest
    @MethodSource("invalidTransferInputs")
    void shouldThrowExceptionForInvalidParamsOnTransfer(UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount) {

        //Act + Assert
        assertThatThrownBy(() -> accountService.transferBetweenAccounts(sourceAccountId, destinationAccountId, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transfer amount must be greater than zero and account IDs must not be null");

        //verify
        verifyNoInteractions(accountRepository, operationRepository);
    }

    private static Stream<Arguments> invalidTransferInputs() {
        UUID sourceValidId = UUID.randomUUID();
        UUID destinationValidId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(2_000);
        return Stream.of(
                Arguments.of(null, destinationValidId, amount), // sourceAccountId = null
                Arguments.of(sourceValidId, null, amount), // destinationAccountId = null
                Arguments.of(sourceValidId, destinationValidId, null), // amount = null
                Arguments.of(sourceValidId, destinationValidId, BigDecimal.ZERO), // amount = 0
                Arguments.of(sourceValidId, destinationValidId, BigDecimal.valueOf(-2)), // amount less to zero
                Arguments.of(null, null, null) // all params null
        );
    }

    @Test
    void shouldThrowExceptionWhenSourceAccountIdIsInvalidOnTransfer() {
        // Arrange
        UUID sourceAccountId = UUID.randomUUID();
        UUID destinationAccountId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(3_000);

        when(accountRepository.findById(sourceAccountId)).thenReturn(Optional.empty());

        //Act + Assert
        assertThatThrownBy(() -> accountService.transferBetweenAccounts(sourceAccountId, destinationAccountId, amount))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found with Id: "+sourceAccountId);

        //verify
        verify(accountRepository).findById(sourceAccountId);
        verifyNoMoreInteractions(accountRepository, operationRepository);
    }

    @Test
    void shouldThrowExceptionWhenDestinationAccountIdIsInvalidOnTransfer() {
        // Arrange
        UUID sourceAccountId = UUID.randomUUID();
        UUID destinationAccountId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(3_000);

        CurrentAccount account = CurrentAccount.builder()
                .accountId(sourceAccountId).build();

        when(accountRepository.findById(sourceAccountId)).thenReturn(Optional.of(account));
        when(accountRepository.findById(destinationAccountId)).thenReturn(Optional.empty());

        //Act + Assert
        assertThatThrownBy(() -> accountService.transferBetweenAccounts(sourceAccountId, destinationAccountId, amount))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found with Id: "+destinationAccountId);

        //verify
        verify(accountRepository).findById(sourceAccountId);
        verify(accountRepository).findById(destinationAccountId);
        verifyNoMoreInteractions(accountRepository, operationRepository);
    }
    
    @Test
    void shouldThrowExceptionWhenSourceAndDestinationAccountIdAreSameOnTransfer() {
        //Arrange
        UUID accountId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(3_000);

        SavingAccount account = SavingAccount.builder()
                .accountId(accountId).build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // Act + Assert
        assertThatThrownBy(() -> accountService.transferBetweenAccounts(accountId, accountId, amount))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Source account must be different from destination account");

        //verify
        verify(accountRepository, times(2)).findById(accountId);
        verifyNoMoreInteractions(accountRepository, operationRepository);
    }

    @Test
    void shouldPerformTransferBetweenAccountsAndRecordOperationWhenValid() {
        // Arrange
        UUID sourceAccountId = UUID.randomUUID();
        UUID destinationAccountId = UUID.randomUUID();
        BigDecimal amountToTransfer = BigDecimal.valueOf(3_000);

        CurrentAccount sourceAccount = CurrentAccount.builder()
                .accountId(sourceAccountId)
                .rib("FR76 1111 3333 3167 6565 374")
                .balance(BigDecimal.valueOf(23_000))
                .createdAt(LocalDateTime.now())
                .status(ACTIVATED)
                .overDraft(BigDecimal.valueOf(200)).build();

        CurrentAccount destinationAccount = CurrentAccount.builder()
                .accountId(destinationAccountId)
                .rib("FR76 0000 2222 3167 6565 987")
                .balance(BigDecimal.valueOf(1_000))
                .createdAt(LocalDateTime.now())
                .status(ACTIVATED)
                .overDraft(BigDecimal.valueOf(200)).build();

        when(accountRepository.findById(sourceAccountId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(destinationAccountId)).thenReturn(Optional.of(destinationAccount));

        // Act
        accountService.transferBetweenAccounts(sourceAccountId, destinationAccountId, amountToTransfer);

        // Verify — debit/credit are delegated to operationService (tested separately in OperationServiceImplTest)
        String expectedDescription = String.format("Transfer from %s to %s", sourceAccountId, destinationAccountId);
        verify(operationService).debitAccount(sourceAccountId, amountToTransfer, expectedDescription);
        verify(operationService).creditAccount(destinationAccountId, amountToTransfer, expectedDescription);

        verify(accountRepository, times(1)).findById(sourceAccountId);
        verify(accountRepository, times(1)).findById(destinationAccountId);
        verifyNoMoreInteractions(accountRepository);
        verifyNoInteractions(operationRepository);
    }

    //----------------------------------GET_ACCOUNTS_PAGINATED-----------------------------------

    @Test
    void shouldReturnPaginatedAccountsWhenParametersAreValid(){
        // Arrange
        int page = 0;
        int size = 5;

        CurrentAccount account = CurrentAccount.builder()
                .accountId(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .balance(BigDecimal.valueOf(20_000))
                .status(ACTIVATED)
                .overDraft(BigDecimal.valueOf(150))
                .rib("FR76 12345 67890 12345678901 34").build();

        SavingAccount account2 = SavingAccount.builder()
                .accountId(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .balance(BigDecimal.valueOf(6_000))
                .status(CREATED)
                .interestRate(BigDecimal.valueOf(1.5))
                .rib("FR76 6666 8888 1245 0000 734").build();

        CurrentAccount account3 = CurrentAccount.builder()
                .accountId(UUID.randomUUID())
                .rib("FR76 1111 3333 3167 6565 374")
                .balance(BigDecimal.valueOf(23_000))
                .createdAt(LocalDateTime.now())
                .status(ACTIVATED)
                .overDraft(BigDecimal.valueOf(200)).build();

        List<BankAccount> accounts = asList(account, account2, account3);
        Page<BankAccount> mockPage = new PageImpl<>(accounts);

        when(accountRepository.findAll(PageRequest.of(page, size))).thenReturn(mockPage);

        // Act
        List<BankAccount> result = accountService.getAccounts(page, size);

        // Assert
        assertThat(result).hasSize(3).contains(account, account2, account3);
    }

    //----------------------------------GET_ACCOUNT_OPERATIONS_PAGE-----------------------------------

    @Test
    void shouldThrowExceptionWhenAccountIdIsInvalidOnGetAccountOperationsPage() {
        int page= 0;
        int size = 5 ;
        UUID accountId = UUID.randomUUID();

        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // Act  + Assert
        assertThatThrownBy(() -> accountService.getAccountOperationsPage(accountId, page, size))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found with Id: "+accountId);

        //verify
        verify(accountRepository, times(1)).findById(accountId);
        verifyNoMoreInteractions(operationRepository);

    }


    @Test
    void shouldThrowExceptionWhenAccountIdIsNullOnGetAccountOperationsPage() {
        int page = 0;
        int size = 5;
        // Act  + Assert
        assertThatThrownBy(() -> accountService.getAccountOperationsPage(null, page, size))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AccountId must not be null");

        //verify
        verifyNoInteractions(operationRepository);

    }

    @Test
    void shouldReturnOperationsPageWhenAllInputsParamsIsValid() {
        //given
        UUID accountId = UUID.randomUUID();
        int page = 0;
        int size = 5;

        CurrentAccount account = CurrentAccount.builder()
                .accountId(accountId).build();

        Operation operation = Operation.builder()
                .operationId(UUID.randomUUID())
                .operationDate(LocalDateTime.now())
                .operationType(CREDIT)
                .operationAmount(BigDecimal.valueOf(10_000))
                .account(CurrentAccount.builder().accountId(accountId).build()).build();

        Operation operation2 = Operation.builder()
                .operationId(UUID.randomUUID())
                .operationType(DEBIT)
                .operationDate(LocalDateTime.now().minusDays(1))
                .operationAmount(BigDecimal.valueOf(100))
                .account(CurrentAccount.builder().accountId(accountId).build()).build();

        Operation operation3 = Operation.builder()
                .operationId(UUID.randomUUID())
                .operationType(DEBIT)
                .operationDate(LocalDateTime.now().minusDays(2))
                .operationAmount(BigDecimal.valueOf(10))
                .account(CurrentAccount.builder().accountId(accountId).build()).build();

        List<Operation> operations = List.of(operation, operation2, operation3);
        Page<Operation> expectedPage = new PageImpl<>(operations);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(operationRepository.findByAccount_AccountId(eq(accountId), any(Pageable.class)))
                .thenReturn(expectedPage);

        // Act
        Page<Operation> result = accountService.getAccountOperationsPage(accountId, page, size);

        // Assert
        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result).hasSize(3),
                () -> assertThat(result).containsExactly(operation, operation2, operation3),
                // ✅ Vérifie que chaque opération appartient bien au même compte
                () -> assertThat(result).allSatisfy(op -> assertThat(op.getAccount().getAccountId()).isEqualTo(accountId)));

        verify(operationRepository, times(1))
                .findByAccount_AccountId(eq(accountId), any(Pageable.class));
    }

    //----------------------------------GET_ACCOUNT_TRANSACTION_HISTORY-----------------------------------

    @Test
    void shouldThrowExceptionWhenAccountIdIsNullOnGetAccountTransactionHistory() {
        // Act  + Assert
        assertThatThrownBy(() -> accountService.getAccountOperations(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AccountId must not be null");

        //verify
        verifyNoInteractions(accountRepository, operationRepository);
    }

    @Test
    void shouldThrowExceptionWhenAccountIdIsInvalidOnGetAccountTransactionHistory() {
        // Act  + Assert
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountOperations(accountId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found with Id: "+accountId);

        //verify
        verify(accountRepository, times(1)).findById(accountId);
        verifyNoMoreInteractions(accountRepository, operationRepository);

    }

    @Test
    void shouldReturnTransactionHistoryWhenAccountIdIsValid() {
        //given
        UUID accountId = UUID.randomUUID();

        CurrentAccount account = CurrentAccount.builder()
                .accountId(accountId).build();

        Operation operation = Operation.builder()
                .operationId(UUID.randomUUID())
                .operationDate(LocalDateTime.now())
                .operationType(CREDIT)
                .operationAmount(BigDecimal.valueOf(10_000))
                .account(CurrentAccount.builder().accountId(accountId).build()).build();

        Operation operation2 = Operation.builder()
                .operationId(UUID.randomUUID())
                .operationType(DEBIT)
                .operationDate(LocalDateTime.now().minusDays(1))
                .operationAmount(BigDecimal.valueOf(100))
                .account(CurrentAccount.builder().accountId(accountId).build()).build();

        Operation operation3 = Operation.builder()
                .operationId(UUID.randomUUID())
                .operationType(DEBIT)
                .operationDate(LocalDateTime.now().minusDays(2))
                .operationAmount(BigDecimal.valueOf(10))
                .account(CurrentAccount.builder().accountId(accountId).build()).build();

        List<Operation> operations = List.of(operation, operation2, operation3);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(operationRepository.findByAccount_AccountId(eq(accountId), any(Sort.class))).thenReturn(operations);

        // Act
        List<Operation> result = accountService.getAccountOperations(accountId);

        // Assert
        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result).hasSize(3),
                () -> assertThat(result).containsExactly(operation, operation2, operation3),
                // Vérifie que chaque opération appartient bien au même compte
                () -> assertThat(result).allSatisfy(op -> assertThat(op.getAccount().getAccountId()).isEqualTo(accountId)));

        //verify
        verify(accountRepository, times(1)).findById(accountId);
        verify(operationRepository, times(1)).findByAccount_AccountId(eq(accountId), any(Sort.class));
    }

    //----------------------------------GET_ACCOUNT_TRANSACTION_HISTORY-----------------------------------

    @Test
    void shouldReturnAllAccountsWithoutPagination() {
        // GIVEN
        BankAccount account1 = CurrentAccount.builder()
                .accountId(UUID.randomUUID())
                .overDraft(BigDecimal.valueOf(150)).build();

        BankAccount account2 = SavingAccount.builder()
                .accountId(UUID.randomUUID())
                .interestRate(BigDecimal.valueOf(1.5)).build();

   List<BankAccount> mockAccounts = List.of(account1, account2);
        when(accountRepository.findAll()).thenReturn(mockAccounts);

        // WHEN
        List<BankAccount> result = accountService.getAllAccounts();

        // THEN
        assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result).containsExactlyElementsOf(mockAccounts)
        );

        verify(accountRepository, times(1)).findAll();
    }

    //----------------------------------DELETE_ACCOUNTS_BY_CUSTOMER-----------------------------------

    @Test
    void shouldDeleteAllAccountsAndTheirOperationsForCustomer() {
        // GIVEN
        UUID customerId = UUID.randomUUID();

        // WHEN
        accountService.deleteAccountsByCustomer(customerId);

        // THEN – operations are bulk-deleted first, then accounts (FK-safe order), in constant queries
        InOrder inOrder = inOrder(operationRepository, accountRepository);
        inOrder.verify(operationRepository, times(1)).deleteByCustomerId(customerId);
        inOrder.verify(accountRepository, times(1)).deleteByCustomerId(customerId);
    }

}
