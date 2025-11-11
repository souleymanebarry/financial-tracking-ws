package com.barry.bank.financial.tracking_ws.services.impl;

import com.barry.bank.financial.tracking_ws.entities.BankAccount;
import com.barry.bank.financial.tracking_ws.entities.CurrentAccount;
import com.barry.bank.financial.tracking_ws.entities.Customer;
import com.barry.bank.financial.tracking_ws.entities.Operation;
import com.barry.bank.financial.tracking_ws.entities.SavingAccount;
import com.barry.bank.financial.tracking_ws.repositories.BankAccountRepository;
import com.barry.bank.financial.tracking_ws.repositories.CustomerRepository;
import com.barry.bank.financial.tracking_ws.repositories.OperationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.barry.bank.financial.tracking_ws.enums.AccountStatus.CREATED;
import static com.barry.bank.financial.tracking_ws.enums.Gender.MALE;
import static com.barry.bank.financial.tracking_ws.enums.OperationType.CREDIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private BankAccountRepository accountRepository;

    @Mock
    private OperationRepository operationRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;


    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .customerId(UUID.randomUUID())
                .firstName("Alexandre")
                .lastName("DURANT")
                .email("alexandre.durant@gmail.com")
                .gender(MALE)
                .build();
    }

    @Test
    void shouldThrowExceptionWhenCustomerIsNullOnCreateCustomer() {

        // Act + Assert
        assertThatThrownBy(() -> customerService.createCustomer(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer must not be null");
        // verify
        verifyNoInteractions(customerRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldThrowExceptionWhenCustomerEmailIsNullOrBlankOnCreateCustomer(String email) {
        // Act + Assert
        Customer customer = Customer.builder()
                .customerId(UUID.randomUUID())
                .firstName("DURANT")
                .lastName("Alexandre")
                .email(email)
                .gender(MALE)
                .build();

        assertThatThrownBy(() -> customerService.createCustomer(customer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer email must not be null");
        // verify
        verifyNoInteractions(customerRepository);
    }


    @Test
    void shouldThrowExceptionWhenCustomerEmailIsAlReadyExistsOnCreateCustomer() {
        final String email = customer.getEmail();
        when(customerRepository.existsByEmailIgnoreCase(email)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> customerService.createCustomer(customer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer with this email already exists");

        // verify
        verify(customerRepository, times(1)).existsByEmailIgnoreCase(email);
        verifyNoMoreInteractions(customerRepository);
    }


    @Test
    void shouldCreateNewCustomerWhenCustomerIsValidOnCreateCustomer() {
        // Arrange
        final String email = customer.getEmail();

        when(customerRepository.existsByEmailIgnoreCase(email)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // Act
        Customer result = customerService.createCustomer(customer);

        // Assert
        assertAll(
                () -> assertThat(result).isSameAs(customer),
                () -> assertThat(result.getEmail()).isSameAs("alexandre.durant@gmail.com")
        );

        // verify
        verify(customerRepository, times(1)).existsByEmailIgnoreCase(email);
        verify(customerRepository, times(1)).save(any(Customer.class));
    }


    @ParameterizedTest
    @MethodSource("invalidCustomerPaginatedInputs")
    void shouldThrowExceptionWhenPageOrSizeIsInvalidOnGetCustomersPaginated(int page, int size) {
        // Act + Assert
        assertThatThrownBy(() -> customerService.getCustomersPaginated(page, size))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page must be greater than or equal to zero, and size must be greater than zero");

        //verify
        verifyNoInteractions(customerRepository);
    }

    private static Stream<Arguments> invalidCustomerPaginatedInputs() {
        return Stream.of(
                Arguments.of(-1, 5),  // invalid page
                Arguments.of(0, 0),   // invalid size
                Arguments.of(-2, 0)   // both invalid
        );
    }

    @Test
    void shouldReturnPaginatedCustomersWhenParamsIsValid() {
        // Arrange
        int page = 0;
        int size = 5;

        Customer customer2 = Customer.builder().build();
        Customer customer3 = Customer.builder().build();
        when(customerRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(customer, customer2, customer3)));

        // Act
        List<Customer> result = customerService.getCustomersPaginated(page, size);

        // Assert
        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result).hasSize(3).contains(customer, customer2, customer3)
        );

        // verify
        verify(customerRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void shouldThrowExceptionWhenNoCustomersFoundInDatabaseOnGetCustomersWithoutPagination() {
        // Arrange
        when(customerRepository.findAll()).thenReturn(List.of());

        // Act + Assert
        assertThatThrownBy(() -> customerService.getCustomersWithoutPagination())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No customers found in the database");

        // verify
        verify(customerRepository, times(1)).findAll();
        verifyNoMoreInteractions(customerRepository);
    }

    @Test
    void shouldReturnCustomersWhenCustomersExistOnGetCustomersWithoutPagination() {
        // Arrange
        Customer customer2 = Customer.builder().build();
        Customer customer3 = Customer.builder().build();
        when(customerRepository.findAll()).thenReturn(List.of(customer, customer2, customer3));

        // Act
        List<Customer> result = customerService.getCustomersWithoutPagination();

        // Assert
        assertAll(
                ()-> assertThat(result).isNotNull(),
                ()-> assertThat(result).hasSize(3).contains(customer, customer2,customer3)
        );

        // verify
        verify(customerRepository, times(1)).findAll();
        verifyNoMoreInteractions(customerRepository);
    }

    @Test
    void shouldThrowExceptionWhenCustomerIdIsInvalidOnGetCustomerById() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> customerService.getCustomerById(customerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer not found with ID:" + customerId);

        // verify
        verify(customerRepository, times(1)).findById(customerId);
    }

    @ParameterizedTest
    @MethodSource("invalidUpdateCustomerInputs")
    void shouldThrowExceptionWhenCustomerIdOrCustomerIsNullOnPartiallyUpdateCustomer(UUID customerId, Customer customer) {

        // Act + Assert
        assertThatThrownBy(() -> customerService.partiallyUpdateCustomer(customerId, customer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer ID and customer data must not be null");


        // verify
        verifyNoInteractions(customerRepository);
    }

    private static Stream<Arguments> invalidUpdateCustomerInputs() {

        Customer customer = Customer.builder().build();
        return Stream.of(
                Arguments.of(UUID.randomUUID(), null),  // invalid customer
                Arguments.of(null, customer),   // invalid customerId
                Arguments.of(null,null)   // both invalid
        );
    }

    @Test
    void shouldThrowExceptionWhenCustomerIdIsInvalidOnPartiallyUpdateCustomer() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> customerService.partiallyUpdateCustomer(customerId, customer))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer not found with ID:" + customerId);

        // verify
        verify(customerRepository, times(1)).findById(customerId);
        verifyNoMoreInteractions(customerRepository);
    }

    @Test
    void shouldReturnPartiallyUpdateCustomerWhenDataIsValid() {
        //  Arrange
        UUID customerId = customer.getCustomerId();

        Customer customerToUpdate = Customer.builder()
                .firstName("Lamine")
                .lastName("CAMARA")
                .build();

        Customer updatedCustomer = Customer.builder()
                .customerId(customerId)
                .firstName("Lamine")
                .lastName("CAMARA")
                .email("alexandre.durant@gmail.com")
                .gender(MALE)
                .build();


        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(updatedCustomer);

        // Act
        Customer result = customerService.partiallyUpdateCustomer(customerId, customerToUpdate);

        // Assert

        assertAll(
                () -> assertThat(result).isNotNull(),
                ()-> assertThat( result.getFirstName()).isEqualTo("Lamine"),
                ()-> assertThat( result.getLastName()).isEqualTo("CAMARA"),
                ()-> assertThat( result.getEmail()).isEqualTo("alexandre.durant@gmail.com"),
                ()-> assertThat( result.getGender()).isEqualTo(MALE)
        );

        //verify
        verify(customerRepository, times(1)).findById(customerId);
        verify(customerRepository, times(1)).save(any(Customer.class));
        verifyNoMoreInteractions(customerRepository);
    }

    @Test
    void shouldThrowExceptionWhenCustomerIdIsInvalidOnDeleteCustomer() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> customerService.deleteCustomer(customerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer not found with ID:" + customerId);

        // verify
        verify(customerRepository, times(1)).findById(customerId);
        verifyNoMoreInteractions(customerRepository);
    }

    @Test
    void shouldDeleteCustomerWithAllRelatedDatWhenCustomerIdIsValid() {
        // Arrange
        UUID customerId = customer.getCustomerId();
        UUID currentAccountId = UUID.randomUUID();
        UUID savingAccountId = UUID.randomUUID();
        UUID operationId1  = UUID.randomUUID();
        UUID operationId2  = UUID.randomUUID();

        CurrentAccount account1 = CurrentAccount.builder()
                .accountId(currentAccountId)
                .rib("FR76 8778 1254 3167 7552 374")
                .balance(BigDecimal.valueOf(20_000))
                .createdAt(LocalDateTime.now())
                .status(CREATED)
                .overDraft(BigDecimal.valueOf(200))
                .customer(customer)
                .build();

        SavingAccount account2 = SavingAccount.builder()
                .accountId(savingAccountId)
                .rib("FR76 2222 1254 3167 7552 374")
                .balance(BigDecimal.valueOf(1_000))
                .createdAt(LocalDateTime.now())
                .status(CREATED)
                .interestRate(BigDecimal.valueOf(1.4))
                .customer(customer)
                .build();

        List<BankAccount> accounts = Arrays.asList(account1, account2);

        Operation.builder()
                .operationId(operationId1)
                .operationDate(LocalDateTime.now())
                .operationType(CREDIT)
                .operationAmount(BigDecimal.valueOf(10_000))
                .account(account1)
                .build();

        Operation.builder()
                .operationId(operationId2)
                .operationDate(LocalDateTime.now())
                .operationType(CREDIT)
                .operationAmount(BigDecimal.valueOf(20_000))
                .account(account1)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(accountRepository.findByCustomer_CustomerId(customerId)).thenReturn(accounts);
        doNothing().when(operationRepository).deleteAllByAccount_AccountId(currentAccountId);
        doNothing().when(operationRepository).deleteAllByAccount_AccountId(savingAccountId);
        doNothing().when(accountRepository).delete(account1);
        doNothing().when(accountRepository).delete(account2);
        doNothing().when(customerRepository).delete(customer);

        // Act
        customerService.deleteCustomer(customerId);

        // Assert
        for (BankAccount account : accounts) {
            verify(operationRepository, times(1)).deleteAllByAccount_AccountId(account.getAccountId());
            verify(accountRepository, times(1)).delete(account);
        }

       verify(customerRepository, times(1)).findById(customerId);
       verify(customerRepository, times(1)).delete(customer);
    }

    @Test
    void shouldGetFullCustomerDataWhenCustomerIdIsValid() {
        // Arrange
        UUID customerId = customer.getCustomerId();
        UUID currentAccountId = UUID.randomUUID();
        UUID savingAccountId = UUID.randomUUID();
        UUID operationId1 = UUID.randomUUID();
        UUID operationId2 = UUID.randomUUID();

        CurrentAccount account1 = CurrentAccount.builder()
                .accountId(currentAccountId)
                .rib("FR76 8778 1254 3167 7552 374")
                .balance(BigDecimal.valueOf(20_000))
                .createdAt(LocalDateTime.now())
                .status(CREATED)
                .overDraft(BigDecimal.valueOf(200))
                .customer(customer)
                .build();

        SavingAccount account2 = SavingAccount.builder()
                .accountId(savingAccountId)
                .rib("FR76 2222 1254 3167 7552 374")
                .balance(BigDecimal.valueOf(1_000))
                .createdAt(LocalDateTime.now())
                .status(CREATED)
                .interestRate(BigDecimal.valueOf(1.4))
                .customer(customer)
                .build();

        List<BankAccount> accounts = Arrays.asList(account1, account2);

        Operation operation1 = Operation.builder()
                .operationId(operationId1)
                .operationDate(LocalDateTime.now())
                .operationType(CREDIT)
                .operationAmount(BigDecimal.valueOf(10_000))
                .account(account1)
                .build();

        Operation operation2 = Operation.builder()
                .operationId(operationId2)
                .operationDate(LocalDateTime.now())
                .operationType(CREDIT)
                .operationAmount(BigDecimal.valueOf(20_000))
                .account(account2)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(accountRepository.findByCustomer_CustomerId(customerId)).thenReturn(accounts);
        when(operationRepository.findByAccount_AccountId(currentAccountId)).thenReturn(List.of(operation1));
        when(operationRepository.findByAccount_AccountId(savingAccountId)).thenReturn(List.of(operation2));

        // Act
        Customer result = customerService.getFullCustomerData(customerId);

        // Assert
        assertThat(result.getCustomerId()).isEqualTo(customerId);

        BankAccount acc1 = result.getBankAccounts().stream()
                .filter(a -> a.getAccountId().equals(currentAccountId))
                .findFirst()
                .orElseThrow();
        BankAccount acc2 = result.getBankAccounts().stream()
                .filter(a -> a.getAccountId().equals(savingAccountId))
                .findFirst()
                .orElseThrow();

        assertThat(acc1.getOperations())
                .hasSize(1)
                .first()
                .satisfies(op -> {
                    assertThat(op.getOperationId()).isEqualTo(operationId1);
                    assertThat(op.getOperationAmount()).isEqualByComparingTo(BigDecimal.valueOf(10_000));
                });

        assertThat(acc2.getOperations())
                .hasSize(1)
                .first()
                .satisfies(op -> {
                    assertThat(op.getOperationId()).isEqualTo(operationId2);
                    assertThat(op.getOperationAmount()).isEqualByComparingTo(BigDecimal.valueOf(20_000));
                });

        // Verify
        verify(customerRepository, times(1)).findById(customerId);
        verify(accountRepository, times(1)).findByCustomer_CustomerId(customerId);
        verify(operationRepository, times(1)).findByAccount_AccountId(currentAccountId);
        verify(operationRepository, times(1)).findByAccount_AccountId(savingAccountId);
    }
}
