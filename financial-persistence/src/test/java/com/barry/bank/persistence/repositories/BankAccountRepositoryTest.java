package com.barry.bank.persistence.repositories;

import com.barry.bank.domain.model.BankAccount;
import com.barry.bank.domain.model.CurrentAccount;
import com.barry.bank.domain.model.Customer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.barry.bank.domain.enumerations.Gender.FEMALE;
import static com.barry.bank.domain.enumerations.Gender.MALE;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("h2")
@ContextConfiguration(classes = BankAccountRepositoryTest.class)
@EntityScan(basePackages = "com.barry.bank.domain.model")
@EnableJpaRepositories(basePackages = "com.barry.bank.persistence.repositories")
class BankAccountRepositoryTest {

    @Autowired
    private BankAccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TestEntityManager entityManager;

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

    @Test
    void shouldDeleteOnlyAccountsOfGivenCustomer() {
        // Arrange
        Customer aisha = customerRepository.save(Customer.builder()
                .firstName("Aisha")
                .lastName("CAMARA")
                .email("aisha.camara@gmail.com")
                .gender(FEMALE)
                .build());
        Customer mohamed = customerRepository.save(Customer.builder()
                .firstName("Mohamed")
                .lastName("SANGARE")
                .email("mohamed.sangare@gmail.com")
                .gender(MALE)
                .build());

        accountRepository.save(currentAccount(aisha, "FR761111111111"));
        accountRepository.save(currentAccount(aisha, "FR762222222222"));
        BankAccount mohamedAccount = accountRepository.save(currentAccount(mohamed, "FR763333333333"));

        // Act
        accountRepository.deleteByCustomerId(aisha.getCustomerId());
        // La suppression bulk contourne le contexte de persistance : on le vide avant de relire
        entityManager.clear();

        // Assert
        assertThat(accountRepository.findByCustomer_CustomerId(aisha.getCustomerId())).isEmpty();
        List<BankAccount> remainingAccounts = accountRepository.findAll();
        assertThat(remainingAccounts)
                .hasSize(1)
                .first()
                .extracting(BankAccount::getAccountId)
                .isEqualTo(mohamedAccount.getAccountId());
    }

    @Test
    void shouldNotDeleteAnythingWhenCustomerHasNoAccount() {
        // Arrange
        Customer aisha = customerRepository.save(Customer.builder()
                .firstName("Aisha")
                .lastName("CAMARA")
                .email("aisha.camara@gmail.com")
                .gender(FEMALE)
                .build());
        Customer mohamed = customerRepository.save(Customer.builder()
                .firstName("Mohamed")
                .lastName("SANGARE")
                .email("mohamed.sangare@gmail.com")
                .gender(MALE)
                .build());
        accountRepository.save(currentAccount(mohamed, "FR763333333333"));

        // Act
        accountRepository.deleteByCustomerId(aisha.getCustomerId());
        entityManager.clear();

        // Assert
        assertThat(accountRepository.findAll()).hasSize(1);
    }

    private CurrentAccount currentAccount(Customer customer, String rib) {
        return CurrentAccount.builder()
                .balance(BigDecimal.valueOf(3_000))
                .overDraft(BigDecimal.valueOf(200))
                .createdAt(LocalDateTime.now())
                .rib(rib)
                .customer(customer)
                .build();
    }
}
