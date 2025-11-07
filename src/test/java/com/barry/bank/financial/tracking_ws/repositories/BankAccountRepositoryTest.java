package com.barry.bank.financial.tracking_ws.repositories;

import com.barry.bank.financial.tracking_ws.entities.CurrentAccount;
import com.barry.bank.financial.tracking_ws.entities.Customer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.barry.bank.financial.tracking_ws.enums.Gender.FEMALE;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("h2")
@DataJpaTest
class BankAccountRepositoryTest {

    @Autowired
    private BankAccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @AfterEach
    void tearDown() {
        accountRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    void shouldReturnTrueWhenRibExists() {
        //Arrange
        Customer customer = Customer.builder()
                .firstName("Aisha")
                .lastName("CAMARA")
                .email("aisha.camara@gmail.com")
                .gender(FEMALE)
                .build();
        customerRepository.save(customer);

        CurrentAccount account = CurrentAccount.builder()
                .balance(BigDecimal.valueOf(3_000))
                .overDraft(BigDecimal.valueOf(200))
                .createdAt(LocalDateTime.now())
                .rib("FR761234567890")
                .customer(customer)
                .build();
        accountRepository.save(account);

        //Act
        boolean exists = accountRepository.existsByRib("FR761234567890");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenRibDoesNotExist() {
        // Act
        boolean exists = accountRepository.existsByRib("FR761234567891");

        // Assert
        assertThat(exists).isFalse();
    }
}
