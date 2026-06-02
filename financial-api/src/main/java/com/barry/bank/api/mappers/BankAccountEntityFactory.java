package com.barry.bank.api.mappers;

import com.barry.bank.api.dtos.AccountDTO;
import com.barry.bank.domain.entities.BankAccount;
import com.barry.bank.domain.entities.CurrentAccount;
import com.barry.bank.domain.entities.SavingAccount;
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
            case BankAccountDtoFactory.CURRENT_ACCOUNT -> new CurrentAccount();
            case BankAccountDtoFactory.SAVING_ACCOUNT  -> new SavingAccount();
            default -> throw new IllegalArgumentException("Unknown account type: " + type);
        };
    }
}
