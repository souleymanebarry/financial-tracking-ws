package com.barry.bank.api.mappers;

import com.barry.bank.api.dtos.CurrentAccountDTO;
import com.barry.bank.domain.entities.CurrentAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {CustomerMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CurrentAccountMapper {

    @Mapping(target = "accountType", constant = BankAccountDtoFactory.CURRENT_ACCOUNT)
    @Mapping(target = "customerDTO", source = "customer")
    CurrentAccountDTO currentAccountToCurrentAccountDto(CurrentAccount currentAccount);

    CurrentAccount currentAccountDtoToCurrentAccount(CurrentAccountDTO currentAccountDto);
}
