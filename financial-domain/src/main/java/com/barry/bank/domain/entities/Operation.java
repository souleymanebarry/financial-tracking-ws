package com.barry.bank.domain.entities;

import com.barry.bank.domain.entities.enums.OperationType;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "account")
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Operation)) return false;
        Operation that = (Operation) o;
        return operationId != null && Objects.equals(operationId, that.operationId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
