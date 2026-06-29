package com.barry.bank.domain.entities;

import com.barry.bank.domain.entities.enums.AccountStatus;
import com.barry.bank.domain.exception.InsufficientBalanceException;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import org.hibernate.annotations.UuidGenerator;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity(name = "Account")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"customer", "operations"})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "ACCOUNT_TYPE", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class BankAccount {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID accountId;

    @Column(unique = true)
    private String rib;

    private BigDecimal balance;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @ManyToOne
    @JoinColumn(name = "CUSTOMER_ID")
    private Customer customer;

    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Operation> operations = new ArrayList<>();

    /**
     * Applies a debit to this account. Enforces the core invariant that the
     * balance can never go below zero.
     *
     * @param amount a strictly positive amount to withdraw
     * @throws IllegalArgumentException    if the amount is null or not strictly positive
     * @throws InsufficientBalanceException if the balance is lower than the amount
     */
    public void debit(BigDecimal amount) {
        requirePositiveAmount(amount);
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(accountId, balance, amount);
        }
        this.balance = balance.subtract(amount);
    }

    /**
     * Applies a credit to this account.
     *
     * @param amount a strictly positive amount to deposit
     * @throws IllegalArgumentException if the amount is null or not strictly positive
     */
    public void credit(BigDecimal amount) {
        requirePositiveAmount(amount);
        this.balance = balance.add(amount);
    }

    private static void requirePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BankAccount)) return false;
        BankAccount that = (BankAccount) o;
        return accountId != null && Objects.equals(accountId, that.accountId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
