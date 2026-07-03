package com.barry.bank.api.mappers;

import com.barry.bank.api.dtos.AccountDTO;
import com.barry.bank.api.dtos.AccountHistoryDTO;
import com.barry.bank.api.dtos.CustomerDTO;
import com.barry.bank.domain.model.CurrentAccount;
import com.barry.bank.domain.model.Customer;
import com.barry.bank.domain.model.Operation;
import com.barry.bank.domain.model.SavingAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.barry.bank.domain.enumerations.AccountStatus.CREATED;
import static com.barry.bank.domain.enumerations.Gender.FEMALE;
import static com.barry.bank.domain.enumerations.OperationType.CREDIT;
import static com.barry.bank.domain.enumerations.OperationType.DEBIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {AccountMapperImpl.class, CustomerMapperImpl.class, OperationMapperImpl.class})
class AccountMapperTest {

    @Autowired
    private AccountMapper accountMapper;

    @Test
    void shouldMapCurrentAccountToDtoWithTypeSpecificFields() {
        // Arrange
        UUID accountId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 1, 9, 0);
        Customer customer = customer();
        CurrentAccount account = CurrentAccount.builder()
                .accountId(accountId)
                .rib("FR761234567890")
                .balance(BigDecimal.valueOf(2500.00))
                .createdAt(createdAt)
                .status(CREATED)
                .customer(customer)
                .overDraft(BigDecimal.valueOf(500.00))
                .build();

        // Act
        AccountDTO dto = accountMapper.accountToAccountDto(account);

        // Assert
        assertAll(
                () -> assertThat(dto.getAccountId()).isEqualTo(accountId),
                () -> assertThat(dto.getRib()).isEqualTo("FR761234567890"),
                () -> assertThat(dto.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(2500.00)),
                () -> assertThat(dto.getCreatedAt()).isEqualTo(createdAt),
                () -> assertThat(dto.getStatus()).isEqualTo(CREATED),
                () -> assertThat(dto.getAccountType()).isEqualTo("CURRENT ACCOUNT"),
                () -> assertThat(dto.getOverDraft()).isEqualByComparingTo(BigDecimal.valueOf(500.00)),
                () -> assertThat(dto.getInterestRate()).isNull(),
                () -> assertThat(dto.getCustomerDTO())
                        .isNotNull()
                        .extracting(CustomerDTO::getLastName)
                        .isEqualTo("CAMARA")
        );
    }

    @Test
    void shouldMapSavingAccountToDtoWithTypeSpecificFields() {
        // Arrange
        SavingAccount account = SavingAccount.builder()
                .accountId(UUID.randomUUID())
                .rib("FR769876543210")
                .balance(BigDecimal.valueOf(10_000.00))
                .status(CREATED)
                .customer(customer())
                .interestRate(BigDecimal.valueOf(2.5))
                .build();

        // Act
        AccountDTO dto = accountMapper.accountToAccountDto(account);

        // Assert
        assertAll(
                () -> assertThat(dto.getAccountType()).isEqualTo("SAVING ACCOUNT"),
                () -> assertThat(dto.getInterestRate()).isEqualByComparingTo(BigDecimal.valueOf(2.5)),
                () -> assertThat(dto.getOverDraft()).isNull()
        );
    }

    @Test
    void shouldMapAccountDtoToCurrentAccountWithoutCustomerNorOperations() {
        // Arrange
        AccountDTO dto = new AccountDTO();
        dto.setAccountId(UUID.randomUUID());
        dto.setRib("FR761112223334");
        dto.setBalance(BigDecimal.valueOf(300.00));
        dto.setAccountType("CURRENT ACCOUNT");
        dto.setOverDraft(BigDecimal.valueOf(150.00));
        dto.setCustomerDTO(new CustomerDTO());

        // Act
        CurrentAccount account = accountMapper.accountDtoToCurrentAccount(dto);

        // Assert
        assertAll(
                () -> assertThat(account.getRib()).isEqualTo("FR761112223334"),
                () -> assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(300.00)),
                () -> assertThat(account.getOverDraft()).isEqualByComparingTo(BigDecimal.valueOf(150.00)),
                // customer et operations sont explicitement ignorés (posés par le service)
                () -> assertThat(account.getCustomer()).isNull(),
                () -> assertThat(account.getOperations()).isEmpty()
        );
    }

    @Test
    void shouldMapAccountDtoToSavingAccountWithoutCustomerNorOperations() {
        // Arrange
        AccountDTO dto = new AccountDTO();
        dto.setRib("FR769999882222");
        dto.setBalance(BigDecimal.valueOf(1500.00));
        dto.setAccountType("SAVING ACCOUNT");
        dto.setInterestRate(BigDecimal.valueOf(2.7));

        // Act
        SavingAccount account = accountMapper.accountDtoToSavingAccount(dto);

        // Assert
        assertAll(
                () -> assertThat(account.getRib()).isEqualTo("FR769999882222"),
                () -> assertThat(account.getInterestRate()).isEqualByComparingTo(BigDecimal.valueOf(2.7)),
                () -> assertThat(account.getCustomer()).isNull(),
                () -> assertThat(account.getOperations()).isEmpty()
        );
    }

    @Test
    void shouldMapAccountHistoryDtoWithPaginationMetadata() {
        // Arrange
        UUID accountId = UUID.randomUUID();
        CurrentAccount account = CurrentAccount.builder()
                .accountId(accountId)
                .balance(BigDecimal.valueOf(3200.00))
                .customer(customer())
                .build();

        Operation op1 = Operation.builder().operationNumber("OP-1").operationType(DEBIT).build();
        Operation op2 = Operation.builder().operationNumber("OP-2").operationType(CREDIT).build();
        // page 0 de taille 2 sur un total de 20 opérations
        PageImpl<Operation> page = new PageImpl<>(List.of(op1, op2), PageRequest.of(0, 2), 20);

        // Act
        AccountHistoryDTO dto = accountMapper.toAccountHistoryDTO(account, page);

        // Assert
        assertAll(
                () -> assertThat(dto.getAccountId()).isEqualTo(accountId),
                () -> assertThat(dto.getAccountHolderName()).isEqualTo("CAMARA"),
                () -> assertThat(dto.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(3200.00)),
                () -> assertThat(dto.getOperations()).hasSize(2),
                () -> assertThat(dto.getCurrentPage()).isZero(),
                () -> assertThat(dto.getPageSize()).isEqualTo(2),
                () -> assertThat(dto.getTotalElements()).isEqualTo(20),
                () -> assertThat(dto.getTotalPages()).isEqualTo(10)
        );
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        assertAll(
                () -> assertThat(accountMapper.accountToAccountDto(null)).isNull(),
                () -> assertThat(accountMapper.accountDtoToCurrentAccount(null)).isNull(),
                () -> assertThat(accountMapper.accountDtoToSavingAccount(null)).isNull()
        );
    }

    private Customer customer() {
        return Customer.builder()
                .customerId(UUID.randomUUID())
                .firstName("Aisha")
                .lastName("CAMARA")
                .email("aisha.camara@gmail.com")
                .gender(FEMALE)
                .build();
    }
}