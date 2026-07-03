package com.barry.bank.document.statement;

import com.barry.bank.domain.enumerations.OperationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StatementLineData(
        LocalDateTime operationDate,
        String operationNumber,
        String label,
        BigDecimal amount,
        OperationType operationType,
        BigDecimal runningBalance
) {}
