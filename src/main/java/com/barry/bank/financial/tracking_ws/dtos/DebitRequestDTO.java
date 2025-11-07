package com.barry.bank.financial.tracking_ws.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DebitRequestDTO {

    @Positive(message = "Amount must be greater than zero")
    @NotNull(message = "Operation amount is required")
    BigDecimal amount;

    @NotBlank(message = "Description must not be blank")
    String description;
}
