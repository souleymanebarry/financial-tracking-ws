package com.barry.bank.domain.model;

import com.barry.bank.domain.enumerations.OperationType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import org.hibernate.annotations.UuidGenerator;
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
@ToString(exclude = "statement")
public class StatementLine {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STATEMENT_ID", nullable = false)
    private BankStatement statement;

    private String operationNumber;

    private LocalDateTime operationDate;

    private String label;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private OperationType operationType;

    private BigDecimal runningBalance;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StatementLine that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
