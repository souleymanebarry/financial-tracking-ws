package com.barry.bank.financial.tracking_ws.dtos;

import com.barry.bank.financial.tracking_ws.enums.AccountStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AccountDTO {

    private UUID accountId;

    private String rib;

    private BigDecimal balance;

    private LocalDateTime createdAt;

    private AccountStatus status;

    private CustomerDTO customerDTO;

    private String accountType;

    private BigDecimal overDraft;

    private BigDecimal interestRate;

}
