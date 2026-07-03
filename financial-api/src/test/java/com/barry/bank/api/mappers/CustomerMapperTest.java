package com.barry.bank.api.mappers;

import com.barry.bank.api.dtos.CustomerDTO;
import com.barry.bank.domain.model.Customer;
import org.junit.jupiter.api.Test;

import java.util.UUID;


import static com.barry.bank.domain.enumerations.Gender.FEMALE;
import static com.barry.bank.domain.enumerations.Gender.MALE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class CustomerMapperTest {

    private final CustomerMapper customerMapper = new CustomerMapperImpl();

    @Test
    void shouldMapCustomerToCustomerDto() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.builder()
                .customerId(customerId)
                .firstName("Aisha")
                .lastName("CAMARA")
                .email("aisha.camara@gmail.com")
                .gender(FEMALE)
                .build();

        // Act
        CustomerDTO dto = customerMapper.customerToCustomerDto(customer);

        // Assert
        assertAll(
                () -> assertThat(dto.getCustomerId()).isEqualTo(customerId),
                () -> assertThat(dto.getFirstName()).isEqualTo("Aisha"),
                () -> assertThat(dto.getLastName()).isEqualTo("CAMARA"),
                () -> assertThat(dto.getEmail()).isEqualTo("aisha.camara@gmail.com"),
                () -> assertThat(dto.getGender()).isEqualTo(FEMALE)
        );
    }

    @Test
    void shouldMapCustomerDtoToCustomerWithoutBankAccounts() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        CustomerDTO dto = new CustomerDTO();
        dto.setCustomerId(customerId);
        dto.setFirstName("Mohamed");
        dto.setLastName("SANGARE");
        dto.setEmail("mohamed.sangare@gmail.com");
        dto.setGender(MALE);

        // Act
        Customer customer = customerMapper.customerDtoToCustomer(dto);

        // Assert
        assertAll(
                () -> assertThat(customer.getCustomerId()).isEqualTo(customerId),
                () -> assertThat(customer.getFirstName()).isEqualTo("Mohamed"),
                () -> assertThat(customer.getLastName()).isEqualTo("SANGARE"),
                () -> assertThat(customer.getEmail()).isEqualTo("mohamed.sangare@gmail.com"),
                () -> assertThat(customer.getGender()).isEqualTo(MALE),
                // bankAccounts est explicitement ignoré par le mapper
                () -> assertThat(customer.getBankAccounts()).isEmpty()
        );
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        assertAll(
                () -> assertThat(customerMapper.customerToCustomerDto(null)).isNull(),
                () -> assertThat(customerMapper.customerDtoToCustomer(null)).isNull()
        );
    }
}
