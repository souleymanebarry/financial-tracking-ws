package com.barry.bank.api.mappers;

import com.barry.bank.api.dtos.AccountDTO;
import com.barry.bank.api.dtos.AccountHistoryDTO;
import com.barry.bank.domain.model.BankAccount;
import com.barry.bank.domain.model.CurrentAccount;
import com.barry.bank.domain.model.Operation;
import com.barry.bank.domain.model.SavingAccount;
import com.barry.bank.domain.enumerations.AccountType;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, uses = {
        CustomerMapper.class,
        OperationMapper.class
})
public abstract class AccountMapper {

    protected OperationMapper operationMapper;

    @Autowired
    public void setOperationMapper(OperationMapper operationMapper) {
        this.operationMapper = operationMapper;
    }

    @Mapping(target = "customerDTO", source = "customer")
    @Mapping(target = "accountType", ignore = true)  // renseignés selon le type concret
    @Mapping(target = "overDraft", ignore = true)    // dans fillTypeSpecificFields
    @Mapping(target = "interestRate", ignore = true)
    public abstract AccountDTO accountToAccountDto(BankAccount account);

    @AfterMapping
    protected void fillTypeSpecificFields(BankAccount account, @MappingTarget AccountDTO dto) {
        if (account instanceof CurrentAccount current) {
            dto.setAccountType(AccountType.CURRENT_ACCOUNT.getLabel());
            dto.setOverDraft(current.getOverDraft());
        } else if (account instanceof SavingAccount saving) {
            dto.setAccountType(AccountType.SAVING_ACCOUNT.getLabel());
            dto.setInterestRate(saving.getInterestRate());
        }
    }

    @Mapping(target = "customer", ignore = true)   // attaché par le service à la création
    @Mapping(target = "operations", ignore = true) // relation inverse, jamais fournie par le client
    public abstract CurrentAccount accountDtoToCurrentAccount(AccountDTO accountDTO);

    @Mapping(target = "customer", ignore = true)   // attaché par le service à la création
    @Mapping(target = "operations", ignore = true) // relation inverse, jamais fournie par le client
    public abstract SavingAccount accountDtoToSavingAccount(AccountDTO accountDTO);

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
