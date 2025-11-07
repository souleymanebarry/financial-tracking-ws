package com.barry.bank.financial.tracking_ws.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class TransferDTO {

    @NotNull(message = "sourceAccountId is required")
    UUID sourceAccountId;

    @NotNull(message = "destinationAccountId is required")
    UUID destinationAccountId;

    @Positive(message = "Amount must be greater than zero")
    @NotNull(message = "Operation amount is required")
    BigDecimal amount;
}
