package com.barry.bank.financial.tracking_ws.entities;

import com.barry.bank.financial.tracking_ws.enums.OperationType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Operation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID operationId;

    private String operationNumber;

    private BigDecimal operationAmount;

    private LocalDateTime operationDate;
    @Enumerated(EnumType.STRING)
    private OperationType operationType;

    private String description;

    @ManyToOne
    @JoinColumn(name = "ACCOUNT_ID")
    private BankAccount account;
}
