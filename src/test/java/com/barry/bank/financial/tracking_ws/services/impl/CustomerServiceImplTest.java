package com.barry.bank.financial.tracking_ws.services.impl;

import static com.barry.bank.financial.tracking_ws.enums.Gender.MALE;
import com.barry.bank.financial.tracking_ws.entities.Customer;
import com.barry.bank.financial.tracking_ws.repositories.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;


    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .customerId(UUID.randomUUID())
                .firstName("DURANT")
                .lastName("Alexandre")
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
    void shouldDeleteCustomerWhenCustomerIdIsValidOnDeleteCustomer() {
        // Arrange
        UUID customerId = customer.getCustomerId();

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        // Act + Assert
       customerService.deleteCustomer(customerId);

        // verify
        verify(customerRepository, times(1)).findById(customerId);
        verify(customerRepository, times(1)).delete(customerCaptor.capture());
        
        // Extract captured arguments
        Customer deletedCustomer = customerCaptor.getValue();

        // Assert
        assertAll(
                () -> assertThat(deletedCustomer.getCustomerId()).isEqualTo(customerId),
                () -> assertThat(deletedCustomer.getEmail()).isEqualTo("alexandre.durant@gmail.com")
        );

    }

}
