package com.barry.bank.financial.tracking_ws.mappers;

import com.barry.bank.financial.tracking_ws.dtos.AccountDTO;
import com.barry.bank.financial.tracking_ws.entities.BankAccount;
import com.barry.bank.financial.tracking_ws.entities.CurrentAccount;
import com.barry.bank.financial.tracking_ws.entities.SavingAccount;
import org.mapstruct.ObjectFactory;
import org.mapstruct.TargetType;
import org.springframework.stereotype.Component;

@Component
public class BankAccountDtoFactory {
    /**
     * Factory utilisée par MapStruct pour créer le bon type de AccountDTO
     * selon la sous-classe de BankAccount.
     */
    @ObjectFactory
    public AccountDTO create(BankAccount account, @TargetType Class<? extends AccountDTO> targetType) {
        if (account == null) {
            return null;
        }

        if (account instanceof CurrentAccount currentAccount) {
            AccountDTO dto = new AccountDTO();
            dto.setAccountType("CURRENT ACCOUNT");
            dto.setOverDraft(currentAccount.getOverDraft());
            return dto;
        } else if (account instanceof SavingAccount savingAccount) {
            AccountDTO dto = new AccountDTO();
            dto.setAccountType("SAVING ACCOUNT");
            dto.setInterestRate(savingAccount.getInterestRate());
            return dto;
        }

        throw new IllegalArgumentException("Unknown account type: " + account.getClass().getSimpleName());
    }

}
