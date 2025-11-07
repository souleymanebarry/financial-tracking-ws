package com.barry.bank.financial.tracking_ws.mappers;

import com.barry.bank.financial.tracking_ws.dtos.CurrentAccountDTO;
import com.barry.bank.financial.tracking_ws.entities.CurrentAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CurrentAccountMapper {

    @Mapping(target = "accountType", constant = "CURRENT ACCOUNT")
    CurrentAccountDTO currentAccountToCurrentAccountDto(CurrentAccount currentAccount);

    CurrentAccount currentAccountDtoToCurrentAccount(CurrentAccountDTO currentAccountDto);
}
