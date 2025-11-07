package com.barry.bank.financial.tracking_ws.repositories;


import com.barry.bank.financial.tracking_ws.entities.Customer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static com.barry.bank.financial.tracking_ws.enums.Gender.MALE;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("h2")
@DataJpaTest
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @AfterEach
    void tearDown() {
        customerRepository.deleteAll();
    }

    @Test
    void shouldReturnTrueWhenEmailExists() {
        // Arrange
        Customer customer = Customer.builder()
                .firstName("Mohamed")
                .lastName("SANGARE")
                .gender(MALE)
                .email("mohamed.sangare@gmail.com")
                .build();
        customerRepository.save(customer);

        // Act
        boolean result = customerRepository.existsByEmailIgnoreCase("mohamed.sangare@gmail.com");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenEmailDoesNotExist() {
        // Arrange
        String unExistingEmail  = "toto@gmail.com";

        // Act
        boolean result = customerRepository.existsByEmailIgnoreCase(unExistingEmail);

        // Assert
        assertThat(result).isFalse();
    }

}
