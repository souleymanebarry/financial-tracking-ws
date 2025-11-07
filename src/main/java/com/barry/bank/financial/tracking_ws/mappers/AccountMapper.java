package com.barry.bank.financial.tracking_ws.mappers;

import com.barry.bank.financial.tracking_ws.dtos.AccountDTO;
import com.barry.bank.financial.tracking_ws.dtos.AccountHistoryDTO;
import com.barry.bank.financial.tracking_ws.entities.BankAccount;
import com.barry.bank.financial.tracking_ws.entities.Operation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring", uses = {BankAccountEntityFactory.class, BankAccountDtoFactory.class, CustomerMapper.class,
        OperationMapper.class})
public interface AccountMapper {

    @Mapping(target = "customer", source = "customerDTO")
    @Mapping(target = "operations", ignore = true)
    BankAccount accountDtoToAccount(AccountDTO accountDTO);

    @Mapping(target = "customerDTO", source = "customer")
    AccountDTO accountToAccountDto(BankAccount account);

    default AccountHistoryDTO toAccountHistoryDTO(BankAccount account, Page<Operation> operationPage,
                                                  OperationMapper operationMapper) {
        AccountHistoryDTO dto = new AccountHistoryDTO();
        dto.setAccountId(account.getAccountId());
        dto.setAccountHolderName(account.getCustomer().getLastName());
        dto.setBalance(account.getBalance());
        dto.setOperations(operationMapper.operationsToOperationDtos(operationPage.getContent()));
        dto.setCurrentPage(operationPage.getNumber());
        dto.setTotalPages(operationPage.getTotalPages());
        dto.setTotalElements(operationPage.getTotalElements());

        return dto;
    }
}
