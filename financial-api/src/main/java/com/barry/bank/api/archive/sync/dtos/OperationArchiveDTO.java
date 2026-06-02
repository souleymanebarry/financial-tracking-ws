package com.barry.bank.api.archive.sync.dtos;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class OperationArchiveDTO {

    private UUID operationId;

    private String operationNumber;

    private BigDecimal operationAmount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime operationDate;

    private String operationType;

    private String description;

}
