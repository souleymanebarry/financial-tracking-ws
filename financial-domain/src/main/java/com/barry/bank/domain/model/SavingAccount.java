package com.barry.bank.domain.model;

import com.barry.bank.domain.enumerations.AccountType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue(value = AccountType.Values.SAVING_ACCOUNT)
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class SavingAccount extends BankAccount {

    private BigDecimal interestRate;

    /**
     * L'identité d'un compte repose uniquement sur son accountId (sémantique entité JPA) :
     * interestRate ne participe pas à l'égalité, on délègue volontairement au parent.
     */
    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
