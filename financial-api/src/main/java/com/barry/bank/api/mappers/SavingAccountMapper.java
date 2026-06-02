package com.barry.bank.api.mappers;

import com.barry.bank.api.dtos.SavingAccountDTO;
import com.barry.bank.domain.entities.SavingAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {CustomerMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SavingAccountMapper {

    @Mapping(target = "accountType", constant = BankAccountDtoFactory.SAVING_ACCOUNT)
    @Mapping(target = "customerDTO", source = "customer")
    SavingAccountDTO savingAccountToSavingAccountDto(SavingAccount savingAccount);

    SavingAccount savingAccountDtoToSavingAccount(SavingAccountDTO savingAccountDto);
}
