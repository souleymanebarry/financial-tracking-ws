package com.barry.bank.document.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record StatementData(
        UUID statementId,
        String accountRib,
        String customerFullName,
        String customerEmail,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDateTime generatedAt,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        List<StatementLineData> lines
) {}