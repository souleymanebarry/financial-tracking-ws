package com.barry.bank.financial.tracking_ws.mappers;

import com.barry.bank.financial.tracking_ws.dtos.SavingAccountDTO;
import com.barry.bank.financial.tracking_ws.entities.SavingAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SavingAccountMapper {

    @Mapping(target = "accountType", constant = "SAVING ACCOUNT")
    SavingAccountDTO savingAccountToSavingAccountDto(SavingAccount savingAccount);

    SavingAccount savingAccountDtoToSavingAccount(SavingAccountDTO savingAccountDto);
}

