package com.barry.bank.financial.tracking_ws.repositories;

import com.barry.bank.financial.tracking_ws.entities.CurrentAccount;
import com.barry.bank.financial.tracking_ws.entities.Customer;
import com.barry.bank.financial.tracking_ws.entities.Operation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.barry.bank.financial.tracking_ws.enums.Gender.FEMALE;
import static com.barry.bank.financial.tracking_ws.enums.OperationType.CREDIT;
import static com.barry.bank.financial.tracking_ws.enums.OperationType.DEBIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@ActiveProfiles("h2")
@DataJpaTest
class OperationRepositoryTest {

    @Autowired
    private OperationRepository operationRepository;

    @Autowired
    private BankAccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private CurrentAccount account;

    @AfterEach
    void tearDown() {
        operationRepository.deleteAll();
        accountRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        Customer customer = Customer.builder()
                .firstName("Aisha")
                .lastName("CAMARA")
                .email("aisha.camara@gmail.com")
                .gender(FEMALE)
                .build();
        customerRepository.save(customer);

        account = CurrentAccount.builder()
                .balance(BigDecimal.valueOf(3_000))
                .overDraft(BigDecimal.valueOf(200))
                .createdAt(LocalDateTime.now())
                .rib("FR761234567890")
                .customer(customer)
                .build();
        accountRepository.save(account);

        Operation op1 = Operation.builder()
                .operationNumber("OP-001")
                .operationAmount(BigDecimal.valueOf(10_000))
                .operationDate(LocalDateTime.now().minusDays(1)) // plus ancien
                .operationType(CREDIT)
                .description("Deposit")
                .account(account)
                .build();
        operationRepository.save(op1);

        Operation op2 = Operation.builder()
                .operationNumber("OP-002")
                .operationAmount(BigDecimal.valueOf(50_000))
                .operationDate(LocalDateTime.now()) // plus récent
                .operationType(DEBIT)
                .description("Withdrawal")
                .account(account)
                .build();
        operationRepository.save(op2);

    }

    @Test
    void shouldReturnAccountOperationsSortedByDateDesc() {
        // Act
        List<Operation> result =
                operationRepository.findByAccount_AccountId(account.getAccountId(), Sort.by(Sort.Direction.DESC, "operationDate"));

        // Assert

        assertThat(result).hasSize(2)
                .extracting(Operation::getOperationNumber)
                .containsExactly("OP-002", "OP-001");
    }

    @Test
    void shouldFindOperationsByAccountIdWithPagination() {
        // Act
        Page<Operation> page = operationRepository.findByAccount_AccountId(account.getAccountId(), PageRequest.of(0, 1));

        // Assert
        assertAll(
                () -> assertThat(page.getContent()).hasSize(1),
                () -> assertThat(page.getTotalElements()).isEqualTo(2),
                () -> assertThat(page.getContent().get(0).getAccount().getAccountId()).isEqualTo(account.getAccountId())
        );
    }

}
