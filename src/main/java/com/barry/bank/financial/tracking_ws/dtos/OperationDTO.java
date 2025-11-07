package com.barry.bank.financial.tracking_ws.dtos;

import com.barry.bank.financial.tracking_ws.enums.OperationType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class OperationDTO {

    private UUID operationId;

    private String operationNumber;

    private BigDecimal operationAmount;

    private LocalDateTime operationDate;

    private OperationType operationType;

    private String description;
}
