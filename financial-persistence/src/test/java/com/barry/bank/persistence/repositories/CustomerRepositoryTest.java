package com.barry.bank.persistence.repositories;



import com.barry.bank.domain.model.Customer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;


import static com.barry.bank.domain.enumerations.Gender.MALE;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("h2")
@DataJpaTest
@ContextConfiguration(classes = CustomerRepositoryTest.class)
@EnableJpaRepositories(basePackages = "com.barry.bank.persistence.repositories")
@EntityScan(basePackages = "com.barry.bank.domain.model")
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
