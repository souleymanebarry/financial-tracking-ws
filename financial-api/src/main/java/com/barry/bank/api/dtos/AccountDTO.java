package com.barry.bank.api.dtos;

import com.barry.bank.domain.enumerations.AccountStatus;
import com.barry.bank.domain.enumerations.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Schema(description = "Bank account information")
public class AccountDTO {

    @Schema(description = "Unique identifier of the account", example = "7ba95f32-1234-4321-abcd-2c963f66afa6", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID accountId;

    @Schema(description = "RIB (bank account number)", example = "FR7612345987650123456789014", accessMode = Schema.AccessMode.READ_ONLY)
    private String rib;

    @Schema(description = "Current balance of the account", example = "1500.00")
    private BigDecimal balance;

    @Schema(description = "Date and time the account was created", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "Current status of the account", example = "CREATED", allowableValues = {"CREATED", "ACTIVATED", "SUSPENDED"})
    private AccountStatus status;

    @Schema(description = "Customer who owns this account")
    private CustomerDTO customerDTO;

    @Schema(description = "Type of the account", example = AccountType.Values.CURRENT_ACCOUNT,
            allowableValues = {AccountType.Values.CURRENT_ACCOUNT, AccountType.Values.SAVING_ACCOUNT})
    private String accountType;

    @Schema(description = "Overdraft limit — applicable to current accounts only", example = "500.00")
    private BigDecimal overDraft;

    @Schema(description = "Annual interest rate — applicable to saving accounts only", example = "3.50")
    private BigDecimal interestRate;

}
