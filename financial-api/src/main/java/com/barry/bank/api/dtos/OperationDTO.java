package com.barry.bank.api.dtos;

import com.barry.bank.domain.enumerations.OperationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Schema(description = "Bank operation (credit, debit or transfer)")
public class OperationDTO {

    @Schema(description = "Unique identifier of the operation", example = "9cf14a21-0001-4abc-beef-aabbccddeeff", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID operationId;

    @Schema(description = "Auto-generated operation reference number", example = "OP-2024-000123", accessMode = Schema.AccessMode.READ_ONLY)
    private String operationNumber;

    @Schema(description = "Amount of the operation", example = "250.00")
    private BigDecimal operationAmount;

    @Schema(description = "Date and time the operation was recorded", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime operationDate;

    @Schema(description = "Type of the operation", example = "CREDIT", allowableValues = {"CREDIT", "DEBIT"})
    private OperationType operationType;

    @Schema(description = "Description or label for the operation", example = "Monthly salary payment")
    private String description;
}
