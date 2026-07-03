package com.barry.bank.api.mappers;

import com.barry.bank.api.dtos.OperationDTO;
import com.barry.bank.domain.model.CurrentAccount;
import com.barry.bank.domain.model.Operation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


import static com.barry.bank.domain.enumerations.OperationType.CREDIT;
import static com.barry.bank.domain.enumerations.OperationType.DEBIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class OperationMapperTest {

    private final OperationMapper operationMapper = new OperationMapperImpl();

    @Test
    void shouldMapOperationToOperationDto() {
        // Arrange
        UUID operationId = UUID.randomUUID();
        LocalDateTime operationDate = LocalDateTime.of(2026, 7, 1, 10, 30);
        Operation operation = Operation.builder()
                .operationId(operationId)
                .operationNumber("OP-20260701-000001")
                .operationAmount(BigDecimal.valueOf(150.00))
                .operationDate(operationDate)
                .operationType(DEBIT)
                .description("Restaurant payment")
                .account(CurrentAccount.builder().accountId(UUID.randomUUID()).build())
                .build();

        // Act
        OperationDTO dto = operationMapper.operationToOperationDto(operation);

        // Assert
        assertAll(
                () -> assertThat(dto.getOperationId()).isEqualTo(operationId),
                () -> assertThat(dto.getOperationNumber()).isEqualTo("OP-20260701-000001"),
                () -> assertThat(dto.getOperationAmount()).isEqualByComparingTo(BigDecimal.valueOf(150.00)),
                () -> assertThat(dto.getOperationDate()).isEqualTo(operationDate),
                () -> assertThat(dto.getOperationType()).isEqualTo(DEBIT),
                () -> assertThat(dto.getDescription()).isEqualTo("Restaurant payment")
        );
    }

    @Test
    void shouldMapOperationDtoToOperationWithoutAccount() {
        // Arrange
        UUID operationId = UUID.randomUUID();
        OperationDTO dto = new OperationDTO();
        dto.setOperationId(operationId);
        dto.setOperationNumber("OP-20260701-000002");
        dto.setOperationAmount(BigDecimal.valueOf(500.00));
        dto.setOperationDate(LocalDateTime.of(2026, 7, 1, 12, 0));
        dto.setOperationType(CREDIT);
        dto.setDescription("Salary deposit");

        // Act
        Operation operation = operationMapper.operationDtoToOperation(dto);


        // Assert
        assertAll(
                () -> assertThat(operation.getOperationId()).isEqualTo(operationId),
                () -> assertThat(operation.getOperationNumber()).isEqualTo("OP-20260701-000002"),
                () -> assertThat(operation.getOperationAmount()).isEqualByComparingTo(BigDecimal.valueOf(500.00)),
                () -> assertThat(operation.getOperationType()).isEqualTo(CREDIT),
                // account est explicitement ignoré par le mapper (relation posée par le service)
                () -> assertThat(operation.getAccount()).isNull()
        );
    }

    @Test
    void shouldMapOperationList() {
        // Arrange
        Operation op1 = Operation.builder().operationNumber("OP-1").operationType(DEBIT).build();
        Operation op2 = Operation.builder().operationNumber("OP-2").operationType(CREDIT).build();

        // Act
        List<OperationDTO> dtos = operationMapper.operationsToOperationDtos(List.of(op1, op2));

        // Assert
        assertThat(dtos)
                .hasSize(2)
                .extracting(OperationDTO::getOperationNumber)
                .containsExactly("OP-1", "OP-2");
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        assertAll(
                () -> assertThat(operationMapper.operationToOperationDto(null)).isNull(),
                () -> assertThat(operationMapper.operationDtoToOperation(null)).isNull(),
                () -> assertThat(operationMapper.operationsToOperationDtos(null)).isNull()
        );
    }
}
