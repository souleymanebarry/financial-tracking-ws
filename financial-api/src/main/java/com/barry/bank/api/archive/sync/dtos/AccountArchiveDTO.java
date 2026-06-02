package com.barry.bank.api.archive.sync.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AccountArchiveDTO {

    private UUID accountId;

    private String rib;

    private BigDecimal balance;

    private String accountType;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    private String status;

    private BigDecimal overDraft;

    private BigDecimal interestRate;

    private List<OperationArchiveDTO> operations;
}
