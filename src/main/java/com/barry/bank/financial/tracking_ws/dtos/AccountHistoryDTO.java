package com.barry.bank.financial.tracking_ws.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class AccountHistoryDTO {

    private UUID accountId;

    private String accountHolderName;

    private BigDecimal balance;

    private List<OperationDTO> operations;

    private int currentPage;

    private int totalPages;

    private int pageSize;

    private long totalElements;
}
