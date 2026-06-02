package com.barry.bank.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Schema(description = "Request body for a transfer between two bank accounts")
public class TransferDTO {

    @NotNull(message = "sourceAccountId is required")
    @Schema(description = "ID of the account to debit", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID sourceAccountId;

    @NotNull(message = "destinationAccountId is required")
    @Schema(description = "ID of the account to credit", example = "7ba95f32-1234-4321-abcd-2c963f66afa6", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID destinationAccountId;

    @Positive(message = "Amount must be greater than zero")
    @NotNull(message = "Operation amount is required")
    @Schema(description = "Amount to transfer — must be strictly positive", example = "500.00", requiredMode = Schema.RequiredMode.REQUIRED)
    BigDecimal amount;
}
