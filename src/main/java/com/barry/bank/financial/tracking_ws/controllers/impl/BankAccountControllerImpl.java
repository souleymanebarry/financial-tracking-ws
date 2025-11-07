package com.barry.bank.financial.tracking_ws.controllers.impl;

import com.barry.bank.financial.tracking_ws.controllers.BankAccountController;
import com.barry.bank.financial.tracking_ws.dtos.AccountDTO;
import com.barry.bank.financial.tracking_ws.dtos.AccountHistoryDTO;
import com.barry.bank.financial.tracking_ws.dtos.CreditRequestDTO;
import com.barry.bank.financial.tracking_ws.dtos.CurrentAccountDTO;
import com.barry.bank.financial.tracking_ws.dtos.DebitRequestDTO;
import com.barry.bank.financial.tracking_ws.dtos.OperationDTO;
import com.barry.bank.financial.tracking_ws.dtos.SavingAccountDTO;
import com.barry.bank.financial.tracking_ws.dtos.TransferDTO;
import com.barry.bank.financial.tracking_ws.entities.BankAccount;
import com.barry.bank.financial.tracking_ws.entities.CurrentAccount;
import com.barry.bank.financial.tracking_ws.entities.Customer;
import com.barry.bank.financial.tracking_ws.entities.Operation;
import com.barry.bank.financial.tracking_ws.entities.SavingAccount;
import com.barry.bank.financial.tracking_ws.mappers.AccountMapper;
import com.barry.bank.financial.tracking_ws.mappers.CurrentAccountMapper;
import com.barry.bank.financial.tracking_ws.mappers.OperationMapper;
import com.barry.bank.financial.tracking_ws.mappers.SavingAccountMapper;
import com.barry.bank.financial.tracking_ws.services.BankAccountService;
import com.barry.bank.financial.tracking_ws.services.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@Log4j2
@RequiredArgsConstructor
public class BankAccountControllerImpl implements BankAccountController {

    private final BankAccountService accountService;
    private final CustomerService customerService;
    private final AccountMapper accountMapper;
    private final OperationMapper operationMapper;
    private final CurrentAccountMapper currentAccountMapper;
    private final SavingAccountMapper savingAccountMapper;

    @Override
    public ResponseEntity<List<AccountDTO>> getAllAccounts() {
        log.info("GET /api/v1/accounts/withoutPaginations");
        List<BankAccount> accounts = accountService.getAccountsWithoutPaginations();
        List<AccountDTO> dtos = accounts.stream()
                .map(accountMapper::accountToAccountDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity<AccountDTO> getAccountById(UUID accountId) {
        log.info("GET /api/v1/accounts/{} ", accountId);
        BankAccount account = accountService.getAccountById(accountId);
        AccountDTO fetchedAccount = accountMapper.accountToAccountDto(account);
        return ResponseEntity.ok(fetchedAccount);
    }

    @Override
    public ResponseEntity<List<AccountDTO>> getAccountsPaginated(int page, int size) {
        log.info("GET /api/v1/accounts?page={}&size={}", page, size);
        List<BankAccount> paginated = accountService.getAccountsPaginated(page, size);
        List<AccountDTO> dtos = paginated.stream().map(accountMapper::accountToAccountDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity<List<OperationDTO>> getAccountOperations(UUID accountId) {
        log.info("GET /api/v1/accounts/{}/operations ", accountId);
        List<Operation> operations = accountService.getAccountTransactionHistory(accountId);
        List<OperationDTO> dtos = operations.stream().map(operationMapper::operationToOperationDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity<AccountHistoryDTO> getAccountHistory(UUID accountId, int page, int size) {
        log.info("GET /api/v1/accounts/{}/history?page={}&size={}", accountId, page, size);

        //Récupération du compte
        BankAccount account = accountService.getAccountById(accountId);

        // 🔹 Récupération paginée des opérations liées à ce compte
        Page<Operation> operationPage = accountService.getAccountOperationsPage(accountId, page, size);

        // 🔹 Conversion en DTO via ton mapper
        AccountHistoryDTO accountHistoryDTO = accountMapper.toAccountHistoryDTO(account, operationPage, operationMapper);
        return ResponseEntity.ok(accountHistoryDTO);
    }

    @Override
    public ResponseEntity<Void> performDebit(UUID accountId, DebitRequestDTO debitDTO) {
        log.info("POST /api/v1/accounts/{}/debit", accountId);
        accountService.debitAccount(accountId, debitDTO.getAmount(), debitDTO.getDescription());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> performCredit(UUID accountId, CreditRequestDTO creditDTO) {
        log.info("POST /api/v1/accounts/{}/credit", accountId);
        accountService.creditAccount(accountId, creditDTO.getAmount(), creditDTO.getDescription());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> performTransfer(TransferDTO transferDTO) {
        log.info("POST /api/v1/accounts/transfer");
        accountService.transferBetweenAccounts(transferDTO.getSourceAccountId(),
                transferDTO.getDestinationAccountId(),
                transferDTO.getAmount());
        return ResponseEntity.ok().build();
    }

    @Override
    public  ResponseEntity<CurrentAccountDTO> createCurrentAccount(UUID customerId, CurrentAccountDTO accountDTO) {
        log.info("POST /api/v1/accounts/{}/current-account", customerId);
        Customer customer = customerService.getCustomerById(customerId);
        CurrentAccount account = currentAccountMapper.currentAccountDtoToCurrentAccount(accountDTO);
        CurrentAccount savedAccount = accountService.createCurrentAccount(account, customer);
        CurrentAccountDTO currentAccountDTO = currentAccountMapper.currentAccountToCurrentAccountDto(savedAccount);

        URI location = URI.create(String.format("api/v1/accounts/%s", savedAccount.getAccountId()));
        return ResponseEntity.created(location).body(currentAccountDTO);
    }

    @Override
    public ResponseEntity<SavingAccountDTO> createSavingAccount(UUID customerId, SavingAccountDTO accountDTO) {
        log.info("POST /api/v1/accounts/{}/saving-account", customerId);
        Customer customer = customerService.getCustomerById(customerId);
        SavingAccount account = savingAccountMapper.savingAccountDtoToSavingAccount(accountDTO);
        SavingAccount savedAccount = accountService.createSavingAccount(account, customer);
        SavingAccountDTO savingAccountDTO = savingAccountMapper.savingAccountToSavingAccountDto(savedAccount);

        URI location = URI.create(String.format("api/v1/accounts/%s", savedAccount.getAccountId()));
        return ResponseEntity.created(location).body(savingAccountDTO);
    }
}
