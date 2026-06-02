package com.barry.bank.api.mappers;

import com.barry.bank.api.dtos.AccountDTO;
import com.barry.bank.api.dtos.AccountHistoryDTO;
import com.barry.bank.api.dtos.CurrentAccountDTO;
import com.barry.bank.api.dtos.SavingAccountDTO;
import com.barry.bank.domain.entities.BankAccount;
import com.barry.bank.domain.entities.CurrentAccount;
import com.barry.bank.domain.entities.Operation;
import com.barry.bank.domain.entities.SavingAccount;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.SubclassMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring", uses = {
        BankAccountEntityFactory.class,
        CustomerMapper.class,
        OperationMapper.class,
        CurrentAccountMapper.class,
        SavingAccountMapper.class
})
public abstract class AccountMapper {

    protected OperationMapper operationMapper;

    @Autowired
    public void setOperationMapper(OperationMapper operationMapper) {
        this.operationMapper = operationMapper;
    }

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "customer", source = "customerDTO")
    @Mapping(target = "operations", ignore = true)
    public abstract BankAccount accountDtoToAccount(AccountDTO accountDTO);

    @SubclassMapping(source = CurrentAccount.class, target = CurrentAccountDTO.class)
    @SubclassMapping(source = SavingAccount.class, target = SavingAccountDTO.class)
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "customerDTO", source = "customer")
    @Mapping(target = "accountType", ignore = true)
    public abstract AccountDTO accountToAccountDto(BankAccount account);

    public AccountHistoryDTO toAccountHistoryDTO(BankAccount account, Page<Operation> operationPage) {
        AccountHistoryDTO dto = new AccountHistoryDTO();
        dto.setAccountId(account.getAccountId());
        dto.setAccountHolderName(account.getCustomer().getLastName());
        dto.setBalance(account.getBalance());
        dto.setOperations(operationMapper.operationsToOperationDtos(operationPage.getContent()));
        dto.setCurrentPage(operationPage.getNumber());
        dto.setTotalPages(operationPage.getTotalPages());
        dto.setPageSize(operationPage.getSize());
        dto.setTotalElements(operationPage.getTotalElements());
        return dto;
    }
}
