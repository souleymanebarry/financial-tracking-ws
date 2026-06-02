package com.barry.bank.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Request body for a credit operation on a bank account")
public class CreditRequestDTO {

    @Positive(message = "Amount must be greater than zero")
    @NotNull(message = "Operation amount is required")
    @Schema(description = "Amount to credit — must be strictly positive", example = "1000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    BigDecimal amount;

    @NotBlank(message = "Description must not be blank")
    @Schema(description = "Label or reason for the credit", example = "Monthly salary", requiredMode = Schema.RequiredMode.REQUIRED)
    String description;
}
