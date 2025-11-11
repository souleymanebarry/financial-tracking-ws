package com.barry.bank.financial.tracking_ws.mappers;

import com.barry.bank.financial.tracking_ws.dtos.AccountDTO;
import com.barry.bank.financial.tracking_ws.entities.BankAccount;
import com.barry.bank.financial.tracking_ws.entities.CurrentAccount;
import com.barry.bank.financial.tracking_ws.entities.SavingAccount;
import org.mapstruct.ObjectFactory;
import org.mapstruct.TargetType;
import org.springframework.stereotype.Component;

@Component
public class BankAccountEntityFactory {

    @ObjectFactory
    public BankAccount create(AccountDTO dto, @TargetType Class<? extends BankAccount> targetType) {
        if (dto == null) {
            return null;
        }

        String type = dto.getAccountType();
        if (type == null) {
            throw new IllegalArgumentException("accountType cannot be null");
        }

        return switch (type.toUpperCase()) {
            case "CURRENT ACCOUNT" -> new CurrentAccount();
            case "SAVING ACCOUNT" -> new SavingAccount();
            default -> throw new IllegalArgumentException("Unknown account type: " + type);
        };
    }
}
