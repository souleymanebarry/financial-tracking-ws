package com.barry.bank.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Request body for a debit operation on a bank account")
public class DebitRequestDTO {

    @Positive(message = "Amount must be greater than zero")
    @NotNull(message = "Operation amount is required")
    @Schema(description = "Amount to debit — must be strictly positive", example = "200.00", requiredMode = Schema.RequiredMode.REQUIRED)
    BigDecimal amount;

    @NotBlank(message = "Description must not be blank")
    @Schema(description = "Label or reason for the debit", example = "ATM withdrawal", requiredMode = Schema.RequiredMode.REQUIRED)
    String description;
}
